package com.healthtracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.R
import com.healthtracker.data.User

class UsersAdapter(
    private val onEditClick: (User) -> Unit,
    private val onDefaultChanged: (User, Boolean) -> Unit
) : ListAdapter<User, UsersAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        private val editUserButton: ImageButton = itemView.findViewById(R.id.editUserButton)
        private val defaultUserCheckBox: CheckBox = itemView.findViewById(R.id.defaultUserCheckBox)

        fun bind(user: User) {
            userNameTextView.text = user.name
            defaultUserCheckBox.isChecked = user.isDefault
            
            editUserButton.setOnClickListener {
                onEditClick(user)
            }
            
            defaultUserCheckBox.setOnClickListener {
                onDefaultChanged(user, defaultUserCheckBox.isChecked)
            }
        }
    }
}

class UserDiffCallback : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem == newItem
    }
}
