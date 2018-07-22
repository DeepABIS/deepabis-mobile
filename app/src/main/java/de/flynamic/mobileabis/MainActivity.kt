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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        val classifier = ImageClassifier(this)

        val bitmap = BitmapFactory.decodeStream(assets.open("apis_mellifera.jpg"))
        val result = classifier.classifyFrame(bitmap)
        val labels = Labels(this).labels

        val top5 = result.slice(IntRange(0, endInclusive = 4))
        for (proposal in top5) {
            Log.d("INFO", "${labels[proposal.first]}: ${proposal.second}")
        }
    }


}
