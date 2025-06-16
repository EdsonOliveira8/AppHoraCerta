import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.DatabaseHelper
import com.example.myapplication.databinding.FragmentCadastrarUsuarioBinding

class FragmentCadastrarUsuario : Fragment() {

    private var _binding: FragmentCadastrarUsuarioBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCadastrarUsuarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())

        // Configuração do Spinner de perfil
        val perfis = arrayOf("funcionario", "admin")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, perfis)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPerfil.adapter = adapter

        binding.btnSalvarUsuario.setOnClickListener {
            val nome = binding.editNome.text.toString()
            val cpf = binding.editCpf.text.toString()
            val senha = binding.editSenha.text.toString()
            val perfil = binding.spinnerPerfil.selectedItem.toString()

            Log.d("Cadastro", "Nome: $nome, CPF: $cpf, Senha: $senha, Perfil: $perfil")

            if (nome.isNotEmpty() && cpf.isNotEmpty() && senha.isNotEmpty()) {
                try {
                    val sucesso = dbHelper.inserirUsuario(nome, cpf, senha, perfil)
                    if (sucesso) {
                        Toast.makeText(requireContext(), "Usuário salvo com sucesso", Toast.LENGTH_SHORT).show()
                        requireActivity().supportFragmentManager.popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "Erro ao salvar. CPF já cadastrado?", Toast.LENGTH_SHORT).show()
                        Log.e("Cadastro", "Erro ao salvar usuário: CPF já cadastrado?")
                    }
                } catch (e: Exception) {
                    // Logando o erro com detalhes
                    Log.e("Cadastro", "Erro na inserção do usuário", e)
                    Toast.makeText(requireContext(), "Erro ao salvar o usuário. Tente novamente.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                Log.w("Cadastro", "Campos incompletos: Nome: $nome, CPF: $cpf, Senha: $senha")
            }
        }

        binding.btnVoltar.setOnClickListener {
            activity?.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
