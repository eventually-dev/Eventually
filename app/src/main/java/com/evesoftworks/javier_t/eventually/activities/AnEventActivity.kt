package com.evesoftworks.javier_t.eventually.activities

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import com.evesoftworks.javier_t.eventually.R
import com.evesoftworks.javier_t.eventually.dbmodel.Event
import com.evesoftworks.javier_t.eventually.dbmodel.User
import com.evesoftworks.javier_t.eventually.interfaces.TaskResultCallback
import com.google.android.gms.location.places.GeoDataClient
import com.google.android.gms.location.places.PlaceDetectionClient
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import com.varunest.sparkbutton.SparkEventListener
import kotlinx.android.synthetic.main.activity_an_event.*
import kotlin.collections.ArrayList

class AnEventActivity : AppCompatActivity(), OnMapReadyCallback, View.OnClickListener, TaskResultCallback {
    lateinit var supportMapFragment: SupportMapFragment
    lateinit var mGeoDataClient: GeoDataClient
    lateinit var mPlaceDetectionClient: PlaceDetectionClient
    lateinit var event: Event
    var assistingFound: Boolean = false
    var taskResultCallback: TaskResultCallback = this
    lateinit var eventsLikedToPush: ArrayList<String>
    lateinit var eventsAssistingToPush: ArrayList<String>
    lateinit var storageReference: StorageReference
    val db = FirebaseFirestore.getInstance()

    override fun onClick(view: View?) {
        when (view!!.id) {
            R.id.fab_event_share -> {

            }

            R.id.assistance_button -> {
                checkIfEventIsAlreadyInFavouritesAndAssistance(event.eventId)
                actionsToEventsAsssistingList(assistingFound)
            }
        }
    }

    override fun onTaskResultGiven(boolean: Boolean) {
        if (boolean) {
            assistance_button.background = ContextCompat.getDrawable(this, R.drawable.rounded_button_cancel)
            assistance_button.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
            assistance_button.text = getString(R.string.assistance_cancel)
        } else {
            assistance_button.background = ContextCompat.getDrawable(this, R.drawable.rounded_button)
            assistance_button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            assistance_button.text = getString(R.string.event_assist)
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        val currentLocLatLng = LatLng(event.latLng.latitude, event.latLng.longitude)

        googleMap!!.addMarker(MarkerOptions().position(currentLocLatLng))

        val cameraPosition: CameraPosition = CameraPosition.Builder()
                .target(currentLocLatLng)
                .zoom(17f)
                .bearing(90f)
                .tilt(30f)
                .build()

        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.setAllGesturesEnabled(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_an_event)

        retrieveEventsListFromCurrentUser()

        mGeoDataClient = Places.getGeoDataClient(this)
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this)

        val bundle = intent.extras
        event = bundle.getParcelable("anEvent")

        checkIfEventIsAlreadyInFavouritesAndAssistance(event.eventId)

        aneventtoolbar.title = event.name
        setSupportActionBar(aneventtoolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        supportMapFragment = map as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        aneventname.text = event.name
        aneventtime.text = event.eventDate
        aneventdescription.text = event.description

        spark_fav.setEventListener(object : SparkEventListener {
            override fun onEventAnimationEnd(button: ImageView?, buttonState: Boolean) {}

            override fun onEventAnimationStart(button: ImageView?, buttonState: Boolean) {}

            override fun onEvent(button: ImageView?, buttonState: Boolean) {
                actionsToEventsLikedList(buttonState)
                spark_fav.playAnimation()
                spark_fav.isChecked = buttonState
            }
        })

        fab_event_share.setOnClickListener(this)
        assistance_button.setOnClickListener(this)

        setPlacePhoto()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return true
    }

    private fun setPlacePhoto() {
        storageReference = FirebaseStorage.getInstance().reference.child("eventsphotos/${event.name}.jpg")

        storageReference.downloadUrl.addOnSuccessListener {
            Picasso.get().load(it).into(aneventimage)
        }
    }

    private fun checkIfEventIsAlreadyInFavouritesAndAssistance(eventId: String) {
        db.collection("Usuarios").document(FirebaseAuth.getInstance().currentUser!!.uid).get().addOnCompleteListener {
            if (it.isSuccessful) {
                val user = it.result.toObject(User::class.java)

                for (event in user!!.eventsLiked) {
                    if (event == eventId) {
                        spark_fav.isChecked = true
                    }
                }

                for (event in user.eventsAssisting) {
                    if (event == eventId) {
                        assistingFound = true
                    }
                }

                taskResultCallback.onTaskResultGiven(assistingFound)
            }
        }
    }

    private fun retrieveEventsListFromCurrentUser() {
        db.collection("Usuarios").document(FirebaseAuth.getInstance().currentUser!!.uid).get().addOnCompleteListener {
            if (it.isSuccessful) {
                val user = it.result.toObject(User::class.java)
                eventsLikedToPush = user!!.eventsLiked
                eventsAssistingToPush = user.eventsAssisting
            }
        }
    }

    private fun actionsToEventsLikedList(state: Boolean) {
        if (state) {
            eventsLikedToPush.add(event.eventId)
        } else {
            eventsLikedToPush.remove(event.eventId)
        }

        performUpdate("eventsLiked", eventsLikedToPush)
    }

    private fun actionsToEventsAsssistingList(state: Boolean) {
        if (state) {
            eventsAssistingToPush.remove(event.eventId)
            assistingFound = false
        } else {
            eventsAssistingToPush.add(event.eventId)
        }

        performUpdate("eventsAssisting", eventsAssistingToPush)
    }

    private fun performUpdate(eventField: String, eventList: ArrayList<String>) {
        db.collection("Usuarios").document(FirebaseAuth.getInstance().currentUser!!.uid).update(eventField, eventList)
    }
}
