package com.android.greentech.plink.fragments.projectileEditor.projectileAdapter

import android.content.Context
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.android.greentech.plink.R
import com.android.greentech.plink.databinding.ItemProjectileBinding
import com.android.greentech.plink.device.projectile.ProjectileData
import com.android.greentech.plink.device.projectile.utils.ProjectilePrefUtils
import com.android.greentech.plink.fragments.dialogs.ProjectileInputDialogFragment

class ProjectileAdapter(context: Context): RecyclerView.Adapter<ProjectileAdapter.ViewHolder>() {
    private var _projectiles = ProjectilePrefUtils.getProjectileListFromPrefs(context)
    private var _projectileSelected = ProjectilePrefUtils.getProjectileSelectedData(context)?.name
    private lateinit var _onItemClickListener: OnItemClickListener
    private var _bindingLast : ItemProjectileBinding ?= null

    init {
        setHasStableIds(true)
    }

    interface OnItemClickListener {
        fun onItemClick(projectile: ProjectileData)
    }

    fun addNewProjectile(context: Context) {
        if(_projectiles.size < MAX_ALLOWED_LIST_SIZE){
            showProjectileEditorDialog(context, ProjectileData(), 0, true)
        }
        else{
            Toast.makeText(
                context,
                "Projectile list limited to $MAX_ALLOWED_LIST_SIZE" as CharSequence,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun resetProjectiles(context: Context) {
        val indexSizePrev = _projectiles.lastIndex
        // Clear the current list
        _projectiles.clear()
        // Notify that projectiles have been removed
        notifyItemRangeRemoved(0, indexSizePrev)
        // Reset to a default projectile list
        _projectiles = ProjectilePrefUtils.setDefaultProjectilesPref(context)
        // Notify that projectiles have been added
        notifyItemRangeInserted(0, _projectiles.lastIndex)
    }

    private fun updateProjectilePrefList(context: Context){
        // Update the projectile list in preferences
        ProjectilePrefUtils.setProjectileListPref(context, _projectiles)
        // Update the selected projectile in preferences
        ProjectilePrefUtils.setProjectileSelectedPref(context, _projectileSelected)
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        if (listener != null) {
            _onItemClickListener = listener
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemProjectileBinding = ItemProjectileBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(parent.context, itemProjectileBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Initialize the projectile data
        val projectile = _projectiles[position]
        val projectileName: Editable = SpannableStringBuilder(projectile.name)
        val projectileWeight: Editable = SpannableStringBuilder(projectile.weight.toString())
        val projectileDiameter: Editable = SpannableStringBuilder(projectile.diameter.toString())
        val projectileDrag: Editable = SpannableStringBuilder(projectile.drag.toString())

        // Show/hide the selection arrow
        if(projectile.name == _projectileSelected){
            holder.itemProjectileBinding.iconArrowSelect.visibility = View.VISIBLE
            _bindingLast = holder.itemProjectileBinding
        }
        else{
            holder.itemProjectileBinding.iconArrowSelect.visibility = View.INVISIBLE
        }
        // Set the projectile name
        if (!TextUtils.isEmpty(projectileName)){
            holder.itemProjectileBinding.projectileName.text = projectileName
        }
        else {
            holder.itemProjectileBinding.projectileName.setText(R.string.unknown_projectile)
        }
        // Set the projectile weight
        holder.itemProjectileBinding.projectileWeight.text = projectileWeight
        // Set the projectile diameter
        holder.itemProjectileBinding.projectileDiameter.text = projectileDiameter
        // Set the projectile drag
        holder.itemProjectileBinding.projectileDrag.text = projectileDrag
    }

    override fun getItemId(position: Int): Long {
        return _projectiles[position].hashCode().toLong()
    }

    override fun getItemCount(): Int {
        return _projectiles.size
    }

    inner class ViewHolder(context: Context, val itemProjectileBinding: ItemProjectileBinding) :
        RecyclerView.ViewHolder(itemProjectileBinding.root) {
        init {
            // Set up the the list item onclick handler
            itemProjectileBinding.projectileContainer.setOnClickListener {
                // Set selected name in list
                _projectileSelected = _projectiles[this.adapterPosition].name
                // Show/hide the selection arrow
                if(_projectiles[this.adapterPosition].name == _projectileSelected){
                    itemProjectileBinding.iconArrowSelect.visibility = View.VISIBLE
                    if(_bindingLast != itemProjectileBinding) {
                        _bindingLast?.iconArrowSelect?.visibility = View.INVISIBLE
                    }
                }
                else{
                    itemProjectileBinding.iconArrowSelect.visibility = View.INVISIBLE
                }
                // Set last binding
                _bindingLast = itemProjectileBinding

                // Call the onClick
                _projectiles[this.adapterPosition]
                    .let { projectile -> _onItemClickListener.onItemClick(projectile) }
            }

            // Set up the Edit button
            itemProjectileBinding.iconEdit.setOnClickListener {
                showProjectileEditorDialog(context, _projectiles[this.adapterPosition], this.adapterPosition, false)
            }

            // Set up the Delete button
            itemProjectileBinding.iconDelete.setOnClickListener {
                removeProjectileFromList(this.adapterPosition)
            }
        }
    }

    private fun showProjectileEditorDialog(context: Context, projectile: ProjectileData, position: Int, isNew: Boolean){
        val listener: ProjectileInputDialogFragment.ProjectileInputDialogListener =
            object : ProjectileInputDialogFragment.ProjectileInputDialogListener {
                override fun onDialogPositiveClick(name: String, weight: Double, diameter: Double, drag: Double) {
                    // Does projectile name already exist in list?
                    val item = _projectiles.find {
                        it.name == name
                    }

                    // Show message indicating projectile already exists
                    if(isNew && item != null){
                        Toast.makeText(
                            context,
                            "Projectile name already exists in list",
                            Toast.LENGTH_SHORT
                        ).show()

                        return
                    }

                    // Update projectile data
                    projectile.setName(name)
                    projectile.setWeight(weight)
                    projectile.setDiameter(diameter)
                    projectile.setDrag(drag)

                    // Is this a new item to add?
                    if(isNew){
                        // Yes - add item to list
                        addProjectileToList(projectile)
                    }
                    else{
                        // No - update item in list
                        updateProjectileInList(position)
                    }
                }
            }

        // Build and show dialog
        try {
            val fragmentManager = (context as FragmentActivity).supportFragmentManager
            ProjectileInputDialogFragment(
                context.getString(R.string.projectile_input_title),
                projectile.name,
                projectile.weight,
                projectile.diameter,
                projectile.drag,
                listener
            ).show(fragmentManager, null)
        } catch (e: ClassCastException) {
            Log.e(TAG, "Cannot find fragment manager")
        }
    }

    private fun addProjectileToList(projectile: ProjectileData){
        _projectiles.add(projectile)
        notifyItemInserted(_projectiles.lastIndex)
    }

    private fun removeProjectileFromList(position: Int){
        _projectiles.removeAt(position)
        notifyItemRemoved(position)
    }

    private fun updateProjectileInList(position: Int){
        notifyItemChanged(position)
    }

    fun onDestroy(context: Context){
        // Make sure to update the projectile data in preferences when exiting
        updateProjectilePrefList(context)
        _bindingLast = null
    }

    companion object{
        const val TAG = "projectile adapter: "
        const val MAX_ALLOWED_LIST_SIZE = 10
    }
}