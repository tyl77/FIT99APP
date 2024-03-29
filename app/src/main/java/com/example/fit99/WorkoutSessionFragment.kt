package com.example.fit99

import AppPreferences
import StepAdapter
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fit99.classes.DoneWorkout
import com.example.fit99.classes.Exercise
import com.example.fit99.classes.Workout
import com.example.fit99.classes.WorkoutExerciseView
import com.google.firebase.firestore.FirebaseFirestore
import douglasspgyn.com.github.circularcountdown.CircularCountdown
import douglasspgyn.com.github.circularcountdown.listener.CircularListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class WorkoutSessionFragment : Fragment() {
    private val db : FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_workout_session, container, false)

        val appPreferences = AppPreferences(requireContext())
        val workout_name: String = requireArguments().getString("workout_name", "My Chest Workout")
        val mycountdown: CircularCountdown = view.findViewById(R.id.mycountdown)

        var set = 0
        var ex = 0
        var main = true
        var sett = false
        var first = true
        var pyramid = true


        //screen
        val screenWidth = resources.displayMetrics.widthPixels
        /*val content2 = view.findViewById<ScrollView>(R.id.content2)
        val layoutParams = content2.layoutParams as ViewGroup.MarginLayoutParams
        val margin = screenWidth
        layoutParams.marginStart = margin
        content2.layoutParams = layoutParams*/

        val content2 = view.findViewById<ScrollView>(R.id.content2)
        content2.translationX = screenWidth.toFloat()

        loadData(
            workout_name,
            appPreferences.getUserEmail().toString(),
            { workout ->
                val name = view.findViewById<TextView>(R.id.name);
                if (workout != null) {
                    name.text = workout.name.toString()
                    if(workout.mode != "Pyramid"){
                        pyramid = false
                    }
                }


            },
            { myexercise ->

                val exercises = ArrayList(myexercise)

                val nameex = view.findViewById<TextView>(R.id.nameex)
                val videoView = view.findViewById<VideoView>(R.id.videoView)
                val steps = view.findViewById<RecyclerView>(R.id.steps);
                val sets = view.findViewById<TextView>(R.id.sets);
                val reps = view.findViewById<TextView>(R.id.reps);
                val pause = view.findViewById<Button>(R.id.Cancel);
                val next = view.findViewById<Button>(R.id.next);
                val label = view.findViewById<TextView>(R.id.label_reps)
                val duration = view.findViewById<TextView>(R.id.duration)

                pause.setOnClickListener {
                    findNavController().navigate(R.id.action_workoutSessionFragment_to_workoutFragment2)
                }

                //content2 is 2nd screen ( used to show rest interval time)
                val content2 = view.findViewById<ScrollView>(R.id.content2)
                steps.layoutManager = LinearLayoutManager(activity)
                steps.setHasFixedSize(true)

                duration.visibility = View.INVISIBLE

                var exercise = exercises.get(0)

                nameex.text = exercise.exerciseName.toString()
                sets.text = (exercise.sets - set).toString()

                steps.adapter = StepAdapter(exercise.steps)
                if(exercise.mode == "Reps"){
                    label.text = "Reps"
                    reps.text = exercise.reps.toString()
                }
                else{
                    label.text = "Duration"
                    reps.text = "${exercise.duration}"
                }

                val videoUri = Uri.parse(exercise.imageUrl + "?alt=media&token=ea2f07c3-17ad-4d1a-907b-6a7c008608cc")
                videoView.setVideoURI(videoUri)
                videoView.setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                    videoView.start()
                }

                if (exercise.mode == "Duration") {
                    duration.visibility = View.VISIBLE
                    mycountdown.create(0, exercises.get(ex).duration.toInt(), CircularCountdown.TYPE_SECOND)
                        .listener(object : CircularListener {
                            override fun onTick(progress: Int) {
                                // Update the TextView with the remaining time
                                val timeRemaining = (exercises.get(ex).duration.toInt() - progress).toString()
                                duration.text = timeRemaining


                            }

                            override fun onFinish(newCycle: Boolean, cycleCount: Int) {
                                duration.text = "0"
                                mycountdown.stop()
                            }
                        })
                        .start()
                }

                var back = false
                var timeshow = false
                var myremove = 2

                var subexercise = WorkoutExerciseView()

                next.setOnClickListener {
                    if (!pyramid) {

                        if(exercises.size == 0 && !main){
                            Toast.makeText(context, "You completed your workout", Toast.LENGTH_SHORT).show()
                            back = true

                            val userID = appPreferences.getUserEmail().toString()
                            val db = FirebaseFirestore.getInstance()
                            db.collection("Users")
                                .whereEqualTo("email", userID)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { documents ->
                                    if (documents.isEmpty) {
                                        Log.d("Firestore", "No user found with this email")
                                    } else {
                                        val userDocRef = documents.documents.first().reference
                                        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                        val doneWorkout = DoneWorkout(date = currentDate, workout = workout_name) // replace with actual workout name

                                        userDocRef.collection("done").add(doneWorkout)
                                            .addOnSuccessListener {
                                                findNavController().navigate(R.id.action_workoutSessionFragment_to_workoutFragment2)
                                                Log.d("Firestore", "Workout successfully marked as done")
                                            }
                                            .addOnFailureListener { e ->
                                                // Handle failure
                                                Log.w("Firestore", "Error adding document", e)
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    // Handle the error
                                    Log.w("Firestore", "Error querying documents", e)
                                }
                        }
                        if (back) {
                            return@setOnClickListener

                        }

                        duration.visibility = View.INVISIBLE

                        val handler = Handler()
                        val delayMillis = 500
                        handler.postDelayed({

                            var nameex = view.findViewById<TextView>(R.id.nameex)
                            var videoView = view.findViewById<VideoView>(R.id.videoView)
                            var steps = view.findViewById<RecyclerView>(R.id.steps);
                            var sets = view.findViewById<TextView>(R.id.sets);
                            var reps = view.findViewById<TextView>(R.id.reps);
                            var pause = view.findViewById<Button>(R.id.Cancel);
                            var next = view.findViewById<Button>(R.id.next);
                            var label = view.findViewById<TextView>(R.id.label_reps)
                            var duration = view.findViewById<TextView>(R.id.duration)

                            if (main) {
                                if (exercises.get(ex).sets==1) {
                                    exercises.remove(exercises.get(ex))
                                }
                                else if(exercises.size==1 && exercises.get(ex).sets == 1){
                                    subexercise = exercises.get(ex)
                                }
                                else{
                                    exercises.get(ex).sets-=1
                                }

                            }

                            var exercise = WorkoutExerciseView()
                            if(exercises.size==0){
                                exercise = subexercise
                            }
                            else{
                                exercise = exercises.get(ex)
                            }

                            sets.text = exercise.sets.toString()

                            if (main) {

                                if (!timeshow) {

                                    duration.visibility = View.INVISIBLE

                                }

                                if (exercises.size == 0) {
                                    return@postDelayed
                                }

                                steps.adapter = StepAdapter(exercise.steps)
                                nameex.text = exercise.exerciseName.toString()

                                if (set == exercise.sets) {
                                    exercises.remove(exercise)
                                }

                                sets.text = (exercise.sets - set).toString()

                                steps.adapter = StepAdapter(exercise.steps)
                                if (exercise.mode == "Reps") {
                                    label.text = "Reps"
                                    reps.text = exercise.reps.toString()
                                } else {
                                    label.text = "Duration"
                                    reps.text = "${exercise.duration}"
                                }
                                val videoUri =
                                    Uri.parse(exercise.imageUrl + "?alt=media&token=ea2f07c3-17ad-4d1a-907b-6a7c008608cc")
                                videoView.setVideoURI(videoUri)
                                videoView.setOnPreparedListener { mediaPlayer ->
                                    mediaPlayer.isLooping = true
                                    mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                                    videoView.start()
                                }

                            } else {

                                if (exercise.mode == "Duration") {
                                    duration.visibility = View.VISIBLE
                                    mycountdown.create(
                                        0,
                                        exercise.duration.toInt(),
                                        CircularCountdown.TYPE_SECOND
                                    )
                                        .listener(object : CircularListener {
                                            override fun onTick(progress: Int) {
                                                // Update the TextView with the remaining time
                                                val timeRemaining =
                                                    (exercise.duration.toInt() - progress).toString()
                                                duration.text = timeRemaining
                                            }
                                            override fun onFinish(
                                                newCycle: Boolean,
                                                cycleCount: Int
                                            ) {
                                                duration.text = "0"
                                                mycountdown.stop()
                                            }
                                        })
                                        .start()
                                }

                                timeshow = true
                            }


                        }, delayMillis.toLong())

                        val content = view.findViewById<ScrollView>(R.id.content)
                        val content2 = view.findViewById<ScrollView>(R.id.content2)

                        if (main) {

                            content.animate().translationX(-screenWidth.toFloat()).setDuration(1000)
                                .start()

                            val circularView: CircularCountdown =
                                view.findViewById(R.id.circularCountdown)
                            val timerTextView: TextView =
                                view.findViewById(R.id.countdownText) // TextView to display time

                        if(!back){
                            circularView.create(
                                0,
                                exercise.interval.toInt(),
                                CircularCountdown.TYPE_SECOND
                            )
                                .listener(object : CircularListener {
                                    override fun onTick(progress: Int) {
                                        // Update the TextView with the remaining time
                                        val timeRemaining =
                                            (exercise.interval.toInt() - progress).toString()
                                        timerTextView.text = timeRemaining


                                    }

                                    override fun onFinish(newCycle: Boolean, cycleCount: Int) {
                                        timerTextView.text = "0"
                                        circularView.stop()
                                    }
                                })
                                .start()

                        }

                            content2.animate().translationX(0f).setDuration(1000).withEndAction {
                                content.translationX = screenWidth.toFloat()
                                main = false

                            }.start()
                        }
                        else if (!main) {
                            content2.animate().translationX(-screenWidth.toFloat())
                                .setDuration(1000).start()

                            content.animate().translationX(0f).setDuration(1000).withEndAction {
                                content2.translationX = screenWidth.toFloat()
                                main = true

                            }.start()
                        }
                    }
                    else{

                        if(back){
                            Toast.makeText(context, "You completed your workout", Toast.LENGTH_SHORT).show()

                            // Assuming you have the userID or a way to get the user's document path
                            val userID = appPreferences.getUserEmail().toString()
                            val db = FirebaseFirestore.getInstance()
                            db.collection("Users")
                                .whereEqualTo("email", userID)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { documents ->
                                    if (documents.isEmpty) {
                                        // Handle case where no documents are found
                                        Log.d("Firestore", "No user found with this email")
                                    } else {
                                        // Assuming you want to use the first document found
                                        val userDocRef = documents.documents.first().reference

                                        // Now you can use 'userDocRef' as needed
                                        // For example, adding a DoneWorkout record:
                                        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                        val doneWorkout = DoneWorkout(date = currentDate, workout = workout_name) // replace with actual workout name

                                        userDocRef.collection("done").add(doneWorkout)
                                            .addOnSuccessListener {
                                                findNavController().navigate(R.id.action_workoutSessionFragment_to_workoutFragment2)
                                                Log.d("Firestore", "Workout successfully marked as done")
                                            }
                                            .addOnFailureListener { e ->
                                                // Handle failure
                                                Log.w("Firestore", "Error adding document", e)
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    // Handle the error
                                    Log.w("Firestore", "Error querying documents", e)
                                }
                        }

                        if (back) {
                            return@setOnClickListener
                        }

                        Log.d("WorkoutSession",pyramid.toString())
                        duration.visibility = View.INVISIBLE

                        val handler = Handler()

                        val delayMillis = 500

                        handler.postDelayed({


                            var nameex = view.findViewById<TextView>(R.id.nameex)

                            var videoView = view.findViewById<VideoView>(R.id.videoView)
                            var steps = view.findViewById<RecyclerView>(R.id.steps);
                            var sets = view.findViewById<TextView>(R.id.sets);
                            var reps = view.findViewById<TextView>(R.id.reps);
                            var pause = view.findViewById<Button>(R.id.Cancel);
                            var next = view.findViewById<Button>(R.id.next);
                            var label = view.findViewById<TextView>(R.id.label_reps)
                            var duration = view.findViewById<TextView>(R.id.duration)



                            if (main) {
                                if (ex + 1 == exercises.size) {
                                    ex = 0
                                    var exerciseIndex = ArrayList<WorkoutExerciseView>()
                                    for(exercise in exercises){
                                        exercise.sets--
                                        if(exercise.sets==0){
                                            exerciseIndex.add(exercise)
                                        }
                                    }
                                    for (exerciseToRemove in exerciseIndex) {
                                        exercises.remove(exerciseToRemove)
                                    }
                                }
                                else if(exercises.size==1 && exercises.get(0).sets==1){
                                    subexercise = exercises.get(0)
                                }
                                else{
                                    ex++
                                }
                            }

                            var exercise = WorkoutExerciseView()

                            if(exercises.size==1 && exercises.get(0).sets==1){
                                exercise = subexercise
                            }
                            else{
                                exercise = exercises.get(ex)
                            }
                            if (main) {

                                if (!timeshow) {

                                    duration.visibility = View.INVISIBLE

                                }


                                steps.adapter = StepAdapter(exercise.steps)
                                nameex.text = exercise.exerciseName.toString()


                                if(exercises.size==1){
                                    sets.text = ((exercise.sets - set)).toString()
                                }
                                else{
                                    sets.text = (exercise.sets - set).toString()
                                }


                                steps.adapter = StepAdapter(exercise.steps)
                                if (exercise.mode == "Reps") {
                                    label.text = "Reps"
                                    reps.text = exercise.reps.toString()
                                } else {
                                    label.text = "Duration"
                                    reps.text = "${exercise.duration}"
                                }
                                val videoUri =
                                    Uri.parse(exercise.imageUrl + "?alt=media&token=ea2f07c3-17ad-4d1a-907b-6a7c008608cc")
                                videoView.setVideoURI(videoUri)
                                videoView.setOnPreparedListener { mediaPlayer ->
                                    mediaPlayer.isLooping = true
                                    mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                                    videoView.start()

                                }

                            } else {

                                if (exercise.mode == "Duration") {

                                    duration.visibility = View.VISIBLE
                                    mycountdown.create(
                                        0,
                                        exercise.duration.toInt(),
                                        CircularCountdown.TYPE_SECOND
                                    )
                                        .listener(object : CircularListener {
                                            override fun onTick(progress: Int) {
                                                // Update the TextView with the remaining time
                                                val timeRemaining =
                                                    (exercise.duration.toInt() - progress).toString()
                                                duration.text = timeRemaining


                                            }

                                            override fun onFinish(
                                                newCycle: Boolean,
                                                cycleCount: Int
                                            ) {
                                                duration.text = "0"
                                                mycountdown.stop()
                                            }
                                        })
                                        .start()
                                }

                                timeshow = true
                            }




                        }, delayMillis.toLong())

                        val content = view.findViewById<ScrollView>(R.id.content)
                        val content2 = view.findViewById<ScrollView>(R.id.content2)

                        if (main && !back) {
                            val exercise = exercises.get(ex)

                            content.animate().translationX(-screenWidth.toFloat()).setDuration(1000)
                                .start()

                            val circularView: CircularCountdown =
                                view.findViewById(R.id.circularCountdown)
                            val timerTextView: TextView =
                                view.findViewById(R.id.countdownText)

                            circularView.create(
                                0,
                                exercises.get(ex).interval.toInt(),
                                CircularCountdown.TYPE_SECOND
                            )
                                .listener(object : CircularListener {
                                    override fun onTick(progress: Int) {
                                        // Update the TextView with the remaining time
                                        val timeRemaining =
                                            (exercise.interval.toInt() - progress).toString()
                                        timerTextView.text = timeRemaining


                                    }

                                    override fun onFinish(newCycle: Boolean, cycleCount: Int) {
                                        timerTextView.text = "0"
                                        circularView.stop()
                                    }
                                })
                                .start()


                            content2.animate().translationX(0f).setDuration(1000).withEndAction {
                                content.translationX = screenWidth.toFloat()
                                main = false


                            }.start()
                        } else if (!main and !back) {

                            content2.animate().translationX(-screenWidth.toFloat())
                                .setDuration(1000).start()


                            // Slide content2 out to the left
                            content.animate().translationX(0f).setDuration(1000).withEndAction {
                                content2.translationX = screenWidth.toFloat()
                                main = true


                            }.start()
                        }
                    }
                }
            }
        )

        return view
    }




    private fun loadData(
        workoutName: String,
        userId: String,
        workoutCallback: (Workout?) -> Unit,
        previewsCallback: (List<WorkoutExerciseView>) -> Unit
    ) {
        Log.d("WorkoutSession", "{$workoutName + $userId}")
        db.collection("Workouts")
            .whereEqualTo("name", workoutName)
            .whereEqualTo("email", userId)
            .get()
            .addOnSuccessListener { documents ->
                var workout: Workout? = null

                for (document in documents) {
                    workout = document.toObject(Workout::class.java)
                    val exercises = workout?.exercises
                    val exercisePreviewsMap = mutableMapOf<String, WorkoutExerciseView>()
                    var fetchCounter = exercises?.size ?: 0

                    exercises?.forEach { exercise ->
                        val exerciseName = exercise.exerciseName

                        db.collection("Exercises")
                            .whereEqualTo("name", exerciseName)
                            .get()
                            .addOnSuccessListener { exerciseDocuments ->
                                for (exerciseDocument in exerciseDocuments) {
                                    val visualURL = exerciseDocument.getString("visualURL")
                                    val steps = exerciseDocument.get("steps") as? ArrayList<String> ?: ArrayList()

                                    if (visualURL != null) {
                                        val exercisePreview = WorkoutExerciseView(
                                            exerciseName = exerciseName,
                                            imageUrl = visualURL,
                                            mode = exercise.mode,
                                            reps = exercise.reps,
                                            duration = exercise.duration,
                                            sets = exercise.sets,
                                            interval = exercise.interval,
                                            steps = steps
                                        )
                                        exercisePreviewsMap[exerciseName] = exercisePreview
                                    }
                                }

                                if (--fetchCounter == 0) {
                                    val orderedPreviews = exercises.mapNotNull {
                                        exercisePreviewsMap[it.exerciseName]
                                    }
                                    previewsCallback(orderedPreviews)
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("FirestoreError", "Error getting exercise documents: ", exception)
                                workoutCallback(null)
                            }
                    }
                }
                workoutCallback(workout)
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreError", "Error getting documents: ", exception)
            }
    }


}