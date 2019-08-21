package com.usersconsentapidemo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import kotlinx.android.synthetic.main.activity_smsverification.*
import java.util.regex.Pattern

class SMSVerificationAct : AppCompatActivity() {
    private val SMS_CONSENT_REQUEST = 2  // Set to an unused request code

    // ---This will match any 6 digit number in the message, can use "|" to lookup more possible combinations
    var pattern: Pattern = Pattern.compile("(|^)\\d{6}")
    // sms broadcast receiver
    private val smsVerificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
                val extras = intent.extras
                val smsRetrieverStatus = extras?.get(SmsRetriever.EXTRA_STATUS) as Status
                when (smsRetrieverStatus.statusCode) {
                    CommonStatusCodes.SUCCESS -> {
                        // Get consent intent
                        val consentIntent = extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                        try {
                            //activity must be started to show consent dialog within 5 minutes
                            // otherwise new timeout intent will be received. 
                            startActivityForResult(consentIntent, SMS_CONSENT_REQUEST)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
                        }
                    }
                    CommonStatusCodes.TIMEOUT -> {
                        // Time out
                        Toast.makeText(applicationContext, "Timeout", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smsverification)
        btn_start.setOnClickListener {
            //        starting SMS User Consent
//        we can also pass specific sender number instead of null.
            val task = SmsRetriever.getClient(this@SMSVerificationAct).startSmsUserConsent(null)
            Toast.makeText(applicationContext, "SMS Consent Started", Toast.LENGTH_LONG).show()
        }

        btn_finish.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        //Registering broadcast receiver to receive broadcast.
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        registerReceiver(smsVerificationReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(smsVerificationReceiver)
    }

    @SuppressLint("SetTextI18n")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SMS_CONSENT_REQUEST ->
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // getting message
                    val message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)

/*  here, message variable will contain a full message. you need to write your logic
to fetch the verification code from the message body.
after getting verification code you can send it to the server or do your further process*/
                    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                    edt_otp.setText(message)
                    tv_msg.text = "Message body : $message"
                    val matcher = pattern.matcher(message)
                    if (matcher.find()) {
                        edt_otp.setText(matcher.group(0))
                    }

                } else {
                    Toast.makeText(applicationContext, "Consent denied, please type manually", Toast.LENGTH_LONG).show()
                    //permission denied. User has to type code manually.
                    edt_otp.isEnabled = true
                }
        }
    }
}