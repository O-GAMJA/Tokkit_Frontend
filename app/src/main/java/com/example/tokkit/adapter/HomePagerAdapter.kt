package com.example.tokkit.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tokkit.fragment.CardViewFragment
import com.example.tokkit.fragment.ListViewFragment
import com.example.tokkit.BubbleChartFragment

class HomePagerAdapter(fragment: Fragment) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CardViewFragment()
            1 -> ListViewFragment()
            2 -> BubbleChartFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}