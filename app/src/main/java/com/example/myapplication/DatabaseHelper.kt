package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "registro_ponto.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                cpf TEXT NOT NULL UNIQUE,
                senha TEXT NOT NULL,
                perfil TEXT NOT NULL CHECK (perfil IN ('admin', 'funcionario'))
            );
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS registros_ponto_diario (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id INTEGER NOT NULL,
                data TEXT NOT NULL,
                entrada TEXT,
                intervalo_inicio TEXT,
                intervalo_fim TEXT,
                saida TEXT,
                latitude_entrada REAL,
                longitude_entrada REAL,
                latitude_saida REAL,
                longitude_saida REAL,
                FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                UNIQUE (usuario_id, data)
            );
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS areas_permitidas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                raio_metros INTEGER NOT NULL
            );
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS ajustes_ponto (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id INTEGER NOT NULL,
                data_hora TEXT NOT NULL,
                tipo TEXT NOT NULL CHECK (tipo IN ('entrada', 'saida', 'intervalo_inicio', 'intervalo_fim')),
                justificativa TEXT,
                status TEXT NOT NULL DEFAULT 'pendente' CHECK (status IN ('pendente', 'aprovado', 'rejeitado')),
                data_solicitacao TEXT NOT NULL,
                FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
            );
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS ajustes_ponto")
        db.execSQL("DROP TABLE IF EXISTS areas_permitidas")
        db.execSQL("DROP TABLE IF EXISTS registros_ponto_diario")
        db.execSQL("DROP TABLE IF EXISTS usuarios")
        onCreate(db)
    }
    fun inserirUsuario(nome: String, cpf: String, senha: String, perfil: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("nome", nome)
            put("cpf", cpf)
            put("senha", senha)
            put("perfil", perfil)
        }

        return try {
            db.insertOrThrow("usuarios", null, values)
            true
        } catch (e: SQLiteConstraintException) {
            Log.e("DB_ERROR", "Violação de restrição (CPF duplicado ou coluna UNIQUE)", e)
            false
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Erro inesperado ao inserir usuário", e)
            false
        } finally {
            db.close()
        }
    }


    // ✅ Ajustado para retornar ID e Nome do usuário
    fun validarLogin(cpf: String, senha: String): Pair<Int, String>? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, nome FROM usuarios WHERE cpf = ? AND senha = ?",
            arrayOf(cpf, senha)
        )

        return if (cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"))
            cursor.close()
            Pair(id, nome)
        } else {
            cursor.close()
            null
        }
    }

    fun getRegistrosPorUsuarioMesAno(usuarioId: Int, ano: Int, mes: Int): List<RegistroPonto> {
        val lista = mutableListOf<RegistroPonto>()
        val db = readableDatabase
        val mesFormatado = if (mes < 10) "0$mes" else "$mes"
        val dataInicio = "$ano-$mesFormatado-01"
        val dataFim = "$ano-$mesFormatado-31"

        val cursor = db.rawQuery("""
            SELECT id, usuario_id, data, entrada, saida,
                   latitude_entrada, longitude_entrada,
                   latitude_saida, longitude_saida
            FROM registros_ponto_diario
            WHERE usuario_id = ? AND data BETWEEN ? AND ?
            ORDER BY data ASC
        """, arrayOf(usuarioId.toString(), dataInicio, dataFim))

        if (cursor.moveToFirst()) {
            do {
                val registro = RegistroPonto(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    usuarioId = cursor.getInt(cursor.getColumnIndexOrThrow("usuario_id")),
                    data = cursor.getString(cursor.getColumnIndexOrThrow("data")),
                    entrada = cursor.getString(cursor.getColumnIndexOrThrow("entrada")),
                    saida = cursor.getString(cursor.getColumnIndexOrThrow("saida")),
                    latitudeEntrada = cursor.getDoubleOrNull(cursor.getColumnIndexOrThrow("latitude_entrada")),
                    longitudeEntrada = cursor.getDoubleOrNull(cursor.getColumnIndexOrThrow("longitude_entrada")),
                    latitudeSaida = cursor.getDoubleOrNull(cursor.getColumnIndexOrThrow("latitude_saida")),
                    longitudeSaida = cursor.getDoubleOrNull(cursor.getColumnIndexOrThrow("longitude_saida"))
                )
                lista.add(registro)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return lista
    }

    // Extensão para tratar valores nulos de Double
    private fun android.database.Cursor.getDoubleOrNull(columnIndex: Int): Double? {
        return if (!isNull(columnIndex)) getDouble(columnIndex) else null
    }

    fun baterPontoUnico(usuarioId: Int): Boolean {
        val db = writableDatabase
        val dataHoje = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val horaAgora = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        try {
            val cursor = db.query(
                "registros_ponto_diario",
                arrayOf("id", "entrada", "saida"),
                "usuario_id = ? AND data = ?",
                arrayOf(usuarioId.toString(), dataHoje),
                null, null, null
            )

            if (cursor.moveToFirst()) {
                val idRegistro = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val entrada = cursor.getString(cursor.getColumnIndexOrThrow("entrada"))
                val saida = cursor.getString(cursor.getColumnIndexOrThrow("saida"))
                cursor.close()

                // Se entrada está vazia (não deveria acontecer porque registro já existe), registra entrada
                if (entrada.isNullOrEmpty()) {
                    val values = ContentValues().apply {
                        put("entrada", horaAgora)
                    }
                    val updatedRows = db.update(
                        "registros_ponto_diario",
                        values,
                        "id = ?",
                        arrayOf(idRegistro.toString())
                    )
                    return updatedRows > 0
                }

                // Se entrada preenchida e saída vazia, registra saída
                if (!entrada.isNullOrEmpty() && saida.isNullOrEmpty()) {
                    val values = ContentValues().apply {
                        put("saida", horaAgora)
                    }
                    val updatedRows = db.update(
                        "registros_ponto_diario",
                        values,
                        "id = ?",
                        arrayOf(idRegistro.toString())
                    )
                    return updatedRows > 0
                }

                // Se entrada e saída preenchidos, já bateu o ponto completo
                return false

            } else {
                // Nenhum registro para o dia: registra a entrada
                cursor.close()
                val values = ContentValues().apply {
                    put("usuario_id", usuarioId)
                    put("data", dataHoje)
                    put("entrada", horaAgora)
                }
                val resultado = db.insert("registros_ponto_diario", null, values)
                return resultado != -1L
            }

        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Erro ao bater ponto: ${e.message}", e)
            return false
        }
    }
    fun inserirEmpresa(nome: String, latitude: Double, longitude: Double, raio: Double): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("nome", nome)                        // String
            put("latitude", latitude)                // Double - OK
            put("longitude", longitude)              // Double - OK
            put("raio", raio)                        // Double - OK
        }
        val result = db.insert("areas_permitidas", null, values)
        return result != -1L
    }

    fun getUltimaBatida(usuarioId: Int): RegistroPonto? {
        val db = readableDatabase

        return try {
            db.rawQuery(
                """
            SELECT id, usuario_id, data, entrada, saida
            FROM registros_ponto_diario
            WHERE usuario_id = ?
            ORDER BY data DESC, entrada DESC
            LIMIT 1
            """.trimIndent(),
                arrayOf(usuarioId.toString())
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    RegistroPonto(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        usuarioId = cursor.getInt(cursor.getColumnIndexOrThrow("usuario_id")),
                        data = cursor.getString(cursor.getColumnIndexOrThrow("data")),
                        entrada = cursor.getString(cursor.getColumnIndexOrThrow("entrada")),
                        saida = cursor.getString(cursor.getColumnIndexOrThrow("saida")),
                        latitudeEntrada = null,
                        longitudeEntrada = null,
                        latitudeSaida = null,
                        longitudeSaida = null
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


}




