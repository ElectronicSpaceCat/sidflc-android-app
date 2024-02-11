package com.android.app.fragments.projectileEditor

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.android.app.R
import com.android.app.databinding.FragmentProjectileBinding
import com.android.app.fragments.projectileEditor.projectileAdapter.ProjectileAdapter
import com.android.app.device.projectile.ProjectileData

class ProjectileEditFragment: Fragment() {
    private var _fragmentProjectileBinding: FragmentProjectileBinding? = null
    private val fragmentProjectileBinding get() = _fragmentProjectileBinding!!
    private var _adapter: ProjectileAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentProjectileBinding = FragmentProjectileBinding.inflate(inflater, container, false)
        return fragmentProjectileBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        // Add menu items without using the Fragment Menu APIs
        // Note how we can tie the MenuProvider to the viewLifecycleOwner
        // and an optional Lifecycle.State (here, RESUMED) to indicate when
        // the menu should be visible
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.activity_drawer_projectile_edit, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.title) {
                    getString(R.string.action_settings_projectile_edit) -> {
                        _adapter?.resetProjectiles(requireContext())
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        // Setup header
        fragmentProjectileBinding.projectileHeader.projectileWeight.text = "Weight\n(g)"
        fragmentProjectileBinding.projectileHeader.projectileDiameter.text = "Diameter\n(mm)"
        fragmentProjectileBinding.projectileHeader.projectileDrag.text = "Drag\n---"
        fragmentProjectileBinding.projectileHeader.iconEdit.visibility = View.INVISIBLE
        fragmentProjectileBinding.projectileHeader.iconDelete.visibility = View.INVISIBLE

        // Configure the recycler view
        fragmentProjectileBinding.recyclerViewProjectiles.layoutManager = LinearLayoutManager(requireActivity())
        fragmentProjectileBinding.recyclerViewProjectiles.addItemDecoration(
            DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL)
        )

        // Configure the recycler view animator
        val animator = fragmentProjectileBinding.recyclerViewProjectiles.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }

        // Setup a listener for when line item is clicked
        val onItemClick: ProjectileAdapter.OnItemClickListener = object :
            ProjectileAdapter.OnItemClickListener {
            override fun onItemClick(projectile: ProjectileData) {
                Toast.makeText(requireContext(), projectile.name + " selected" as CharSequence, Toast.LENGTH_SHORT).show()
            }
        }

        _adapter = ProjectileAdapter(requireContext())
        _adapter?.setOnItemClickListener(onItemClick)

        // Set up the floating-action-button (fab)
        fragmentProjectileBinding.btnAddProjectile.setOnClickListener {
            _adapter?.addNewProjectile(requireContext())
        }

       // Set recyclerView adapter to adapter instance
        fragmentProjectileBinding.recyclerViewProjectiles.adapter = _adapter
    }

    override fun onDestroyView() {
        _adapter?.onDestroy(requireContext())
        _adapter = null
        _fragmentProjectileBinding = null
        super.onDestroyView()
    }
}
