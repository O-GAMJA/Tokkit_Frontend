package com.example.tokkit.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tokkit.fragment.CardViewFragment
import com.example.tokkit.fragment.ListViewFragment

class HomePagerAdapter(fragment: Fragment) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CardViewFragment()
            1 -> ListViewFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}