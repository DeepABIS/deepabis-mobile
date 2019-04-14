package de.flynamic.mobileabis

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.util.Log
import de.flynamic.mobileabis.inference.ImageClassifier
import de.flynamic.mobileabis.inference.Labels
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), SpeciesFragment.OnListFragmentInteractionListener, PredictFragment.OnFragmentInteractionListener {
    private val speciesFragment = SpeciesFragment()
    private val predictFragment = PredictFragment()

    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onListFragmentInteraction(item: SpeciesFragment.SpeciesItem?) {

    }


    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_predict -> {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, predictFragment).commit()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_species -> {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, speciesFragment).commit()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    public lateinit var classifier: ImageClassifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        classifier = ImageClassifier(assets)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, predictFragment).commit()

    }


}
