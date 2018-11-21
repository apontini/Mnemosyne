package ap.mnemosyne.activities

import android.app.TimePickerDialog
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
        setSupportActionBar(toolbar)

        var fromTime : LocalTime
        var toTime : LocalTime

        val toTimePickListener = TimePickerDialog.OnTimeSetListener { p0, p1, p2 ->
            toTime = LocalTime(p1, p2)
        }

        val fromTimePickListener = TimePickerDialog.OnTimeSetListener { p0, p1, p2 ->
            fromTime = LocalTime(p1, p2)

            picker2 = TimePickerDialog(this, toTimePickListener, 12, 0, true)
            picker2.setCanceledOnTouchOutside(false)
            picker2.setTitle("Scegli il tempo di fine")
            picker2.setOnCancelListener { picker2.show() }
            picker2.show()
        }

        picker = TimePickerDialog(this, fromTimePickListener, 12, 0, true)
        picker.setCanceledOnTouchOutside(false)
        picker.setTitle("Scegli il tempo d'inizio")
        picker.setOnCancelListener { picker.show() }
        picker.show()
    }
}
