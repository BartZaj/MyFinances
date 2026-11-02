package com.example.myfinances

import android.content.Intent
import android.os.Bundle
import android.os.Trace
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class CategoriesLauncherFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Trace.beginSection("XMLScreen_CategoriesLauncher")

        // Ten widok powinieneś stworzyć oddzielnie, np. fragment_categories_launcher.xml (jeden przycisk na środku)
        val view = inflater.inflate(R.layout.fragment_categories_launcher, container, false)

        val btnLaunch = view.findViewById<Button>(R.id.btnLaunchCategories)
        btnLaunch.setOnClickListener {
            // Bezpośrednie wywołanie klasycznej Aktywności ExpandableCategoriesXmlActivity
            val intent = Intent(requireContext(), ExpandableCategoriesXmlActivity::class.java)
            startActivity(intent)
        }

        Trace.endSection()
        return view
    }
}