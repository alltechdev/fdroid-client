package com.atd.store.utility.extension

import androidx.fragment.app.Fragment
import com.atd.store.MainActivity

inline val Fragment.mainActivity: MainActivity
    get() = requireActivity() as MainActivity
