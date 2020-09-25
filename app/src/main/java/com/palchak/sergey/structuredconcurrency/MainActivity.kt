package com.palchak.sergey.structuredconcurrency

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "AppDebug"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main()
    }

    private val handler = CoroutineExceptionHandler { _, exception ->
        println("Exception thrown in one of the children: $exception")
    }

    private fun main() {
        val parentJob = CoroutineScope(IO).launch {

            supervisorScope {
                // ---------- JOB A ----------
                val jobA = launch {
                    val resultA = getResult(1)
                    println("ResultA: $resultA")
                }
                jobA.invokeOnCompletion {
                    if (it != null) {
                        println("Error getting resultA: $it")
                    }
                }

                // ---------- JOB B ----------
                val jobB = launch((handler)) {
                    val resultB = getResult(2)
                    println("ResultB: $resultB")
                }
                jobB.invokeOnCompletion {
                    if (it != null) {
                        println("Error getting resultB: $it")
                    }
                }

                // ---------- JOB C ----------
                val jobC = launch {
                    val resultC = getResult(3)
                    println("ResultC: $resultC")
                }
                jobC.invokeOnCompletion {
                    if (it != null) {
                        println("Error getting resultC: $it")
                    }
                }
            }
        }
        parentJob.invokeOnCompletion {
            if (it != null) {
                println("Parent job failed: $it")
            } else println("Parent job SUCCESS")
        }
    }

    private suspend fun getResult(number: Int): Int {
        return withContext(Main) {
            delay(number * 500L)
            if (number == 2) {
                // exception in child propagates to the parent job and cancel all jobs starting from
                // matching one, previous jobs remain completed
                throw Exception("Error getting result for number: $number")
                // but cancellationException cancels only the jobs that match the if condition
//                throw CancellationException("Error getting result for number: $number")
            }
            number * 2
        }
    }

    private fun println(message: String) {
        Log.d(TAG, message)
    }
}