package ap.mnemosyne.activities

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity;
import apontini.mnemosyne.R

import kotlinx.android.synthetic.main.activity_time_interval_picker.*
import org.joda.time.LocalTime

class TimeIntervalPickerActivity : AppCompatActivity()
{

    private lateinit var picker : TimePickerDialog
    private lateinit var picker2 : TimePickerDialog

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_interval_picker)

        lateinit var fromTime : LocalTime
        lateinit var toTime : LocalTime

        val toTimePickListener = TimePickerDialog.OnTimeSetListener { _, p1, p2 ->
            toTime = LocalTime(p1, p2)
            val retIntent = Intent()
            retIntent.putExtra("fromTime", fromTime)
            retIntent.putExtra("toTime", toTime)
            setResult(Activity.RESULT_OK, retIntent)
            finish()
        }

        val fromTimePickListener = TimePickerDialog.OnTimeSetListener { _, p1, p2 ->
            fromTime = LocalTime(p1, p2)

            picker2 = TimePickerDialog(this, toTimePickListener, 12, 0, true)
            picker2.setTitle("Scegli il tempo di fine")
            picker2.setOnCancelListener {
                val retIntent = Intent()
                setResult(Activity.RESULT_CANCELED, retIntent)
                finish()
            }
            picker2.show()
        }

        picker = TimePickerDialog(this, fromTimePickListener, 12, 0, true)
        picker.setTitle("Scegli il tempo d'inizio")
        picker.setOnCancelListener {
            val retIntent = Intent()
            setResult(Activity.RESULT_CANCELED, retIntent)
            finish()
        }
        picker.show()
    }
}
