package com.example.unleash

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.unleash.databinding.FragmentSecondBinding
import dagger.hilt.android.AndroidEntryPoint
import io.getunleash.UnleashClient
import javax.inject.Inject

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
@AndroidEntryPoint
class SecondFragment : Fragment() {

    @Inject
    lateinit var unleashClient: UnleashClient

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            Log.w("unleash", "unleashClient.isEnabled(\"unleash_second_fragment\") is " + unleashClient.isEnabled("unleash_second_fragment"))
            if (unleashClient.isEnabled("unleash_second_fragment")) {
                findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}