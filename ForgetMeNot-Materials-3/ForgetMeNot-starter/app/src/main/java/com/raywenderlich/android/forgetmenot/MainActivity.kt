/*
 * Copyright (c) 2017 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.forgetmenot

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.content.IntentFilter
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import java.util.*

class MainActivity : AppCompatActivity() {

  private val taskList = mutableListOf<String>()
  private val adapter by lazy { makeAdapter(taskList) }
  private val ADD_TASK_REQUEST = 1
  private val tickReceiver by lazy { makeBroadcastReceiver() }
  private val PREFS_TASKS = "prefs_tasks"
  private val KEY_TASKS_LIST = "tasks_list"

  companion object {
    private const val LOG_TAG = "MainActivityLog"

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getCurrentTimeStamp(): String {
      val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
      val now = Date()
      return simpleDateFormat.format(now)
    }
  }

  private fun makeBroadcastReceiver(): BroadcastReceiver {
    return object : BroadcastReceiver() {
      @RequiresApi(Build.VERSION_CODES.N)
      override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_TIME_TICK) {
          dateTimeTextView.text = getCurrentTimeStamp()
        }
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    taskListView.adapter = adapter

    taskListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
      taskSelected(position)
    }

    val savedList = getSharedPreferences(PREFS_TASKS, Context.MODE_PRIVATE).getString(KEY_TASKS_LIST, null)
    if (savedList != null) {
      val items = savedList.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      taskList.addAll(items)
    }
  }

  @RequiresApi(Build.VERSION_CODES.N)
  override fun onResume() {
    // 1
    super.onResume()
    // 2
    dateTimeTextView.text = getCurrentTimeStamp()
    // 3
    registerReceiver(tickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
  }

  override fun onPause() {
    // 4
    super.onPause()
    // 5
    try {
      unregisterReceiver(tickReceiver)
    } catch (e: IllegalArgumentException) {
      Log.e(MainActivity.LOG_TAG, "Time tick Receiver not registered", e)
    }
  }

  override fun onStop() {
    super.onStop()

    // Save all data which you want to persist.
    val savedList = StringBuilder()
    for (task in taskList) {
      savedList.append(task)
      savedList.append(",")
    }

    getSharedPreferences(PREFS_TASKS, Context.MODE_PRIVATE).edit()
      .putString(KEY_TASKS_LIST, savedList.toString()).apply()
  }

  override fun onConfigurationChanged(newConfig: Configuration?) {
    super.onConfigurationChanged(newConfig)
  }


  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    // 1
    if (requestCode == ADD_TASK_REQUEST) {
      // 2
      if (resultCode ==   Activity.RESULT_OK) {
        // 3
        val task = data?.getStringExtra(TaskDescriptionActivity.EXTRA_TASK_DESCRIPTION)
        task?.let {
          taskList.add(task)
          // 4
          adapter.notifyDataSetChanged()
        }
      }
    }
  }


  fun addTaskClicked(view: View) {
    val intent = Intent(this, TaskDescriptionActivity::class.java)
    startActivityForResult(intent, ADD_TASK_REQUEST)

  }

  private fun makeAdapter(list: List<String>): ArrayAdapter<String> =
      ArrayAdapter(this, android.R.layout.simple_list_item_1, list)


  private fun taskSelected(position: Int) {
    // 1
    AlertDialog.Builder(this)
      // 2
      .setTitle(R.string.alert_title)
      // 3
      .setMessage(taskList[position])
      .setPositiveButton(R.string.delete, { _, _ ->
        taskList.removeAt(position)
        adapter.notifyDataSetChanged()
      })
      .setNegativeButton(R.string.cancel, {
          dialog, _ -> dialog.cancel()
      })
      // 4
      .create()
      // 5
      .show()
  }
}
