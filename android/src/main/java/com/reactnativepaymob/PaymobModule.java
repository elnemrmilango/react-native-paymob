package com.reactnativepaymob;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;

import com.paymob.acceptsdk.IntentConstants;
import com.paymob.acceptsdk.PayActivity;
import com.paymob.acceptsdk.PayActivityIntentKeys;
import com.paymob.acceptsdk.PayResponseKeys;
import com.paymob.acceptsdk.SaveCardResponseKeys;


import java.util.HashMap;

public class PaymobModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;
  int REQUEST_CODE = 30767;

  public PaymobModule(ReactApplicationContext reactContext) {
    super(reactContext);
    reactContext.addActivityEventListener(mActivityEventListener);
    this.reactContext = reactContext;
  }

  private void sendEvent(ReactContext reactContext, String eventName, WritableMap params) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
      .emit(eventName, params);
  }


  private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
      super.onActivityResult(activity, requestCode, resultCode, data);


      if (requestCode == REQUEST_CODE) {
        Bundle extras = data.getExtras();
        WritableMap params = Arguments.createMap();

        WritableMap savedCardData = Arguments.createMap();

        WritableMap payData = Arguments.createMap();

          if (extras != null) {
            for (String key : extras.keySet()) {
              Object value = extras.get(key);
                if (value instanceof Float || value instanceof Double) {
                    payData.putDouble(key, extras.getDouble(key));
                } else if (value instanceof Number) {
                    payData.putInt(key, extras.getInt(key));
                } else if (value instanceof String) {
                    payData.putString(key, extras.getString(key));
                } else if (value instanceof Boolean) {
                    payData.putBoolean(key, extras.getBoolean(key));
                }
              }

                if(payData.hasKey("token")){
                    savedCardData.putString("id",extras.getString(SaveCardResponseKeys.ID));
                    savedCardData.putString("token", extras.getString(SaveCardResponseKeys.TOKEN));
                    savedCardData.putString("card_subtype", extras.getString(SaveCardResponseKeys.CARD_SUBTYPE));
                    savedCardData.putString("masked_pan", extras.getString(SaveCardResponseKeys.MASKED_PAN));
                    savedCardData.putString("merchant_id", extras.getString(SaveCardResponseKeys.MERCHANT_ID));
                    savedCardData.putString("email", extras.getString(SaveCardResponseKeys.EMAIL));
                }
            }


        if (resultCode == IntentConstants.USER_CANCELED) {
          // User canceled and did no payment request was fired
          // ToastMaker.displayShortToast(this, "User canceled!!");
          params.putString("type", "userDidCancel");
          sendEvent(reactContext, "didDismiss", params);
        } else if (resultCode == IntentConstants.MISSING_ARGUMENT) {
          // You forgot to pass an important key-value pair in the intent's extras
          // ToastMaker.displayShortToast(this, "Missing Argument == " + extras.getString(IntentConstants.MISSING_ARGUMENT_VALUE));
        } else if (resultCode == IntentConstants.TRANSACTION_ERROR) {
          params.putString("type", "paymentAttemptFailed");
          params.putString("detailedDescription", extras.getString(IntentConstants.TRANSACTION_ERROR_REASON));
          sendEvent(reactContext, "didDismiss", params);
        } else if (resultCode == IntentConstants.TRANSACTION_REJECTED) {
          // User attempted to pay but their transaction was rejected


          params.putString("type", "transactionRejected");
          sendEvent(reactContext, "didDismiss", params);
          // Use the static keys declared in PayResponseKeys to extract the fields you want
          // ToastMaker.displayShortToast(this, extras.getString(PayResponseKeys.DATA_MESSAGE));
        } else if (resultCode == IntentConstants.TRANSACTION_REJECTED_PARSING_ISSUE) {
          // User attempted to pay but their transaction was rejected. An error occured while reading the returned JSON
          // ToastMaker.displayShortToast(this, extras.getString(IntentConstants.RAW_PAY_RESPONSE));
        } else if (resultCode == IntentConstants.TRANSACTION_SUCCESSFUL) {
          // User finished their payment successfully

          // Use the static keys declared in PayResponseKeys to extract the fields you want
          // ToastMaker.displayShortToast(this, extras.getString(PayResponseKeys.DATA_MESSAGE));
          params.putString("type", "transactionAccepted");

          params.putMap("payData", payData);

          sendEvent(reactContext, "didDismiss", params);
        } else if (resultCode == IntentConstants.TRANSACTION_SUCCESSFUL_PARSING_ISSUE) {
          // User finished their payment successfully. An error occured while reading the returned JSON.
          // ToastMaker.displayShortToast(this, "TRANSACTION_SUCCESSFUL - Parsing Issue");

          // ToastMaker.displayShortToast(this, extras.getString(IntentConstants.RAW_PAY_RESPONSE));
        } else if (resultCode == IntentConstants.TRANSACTION_SUCCESSFUL_CARD_SAVED) {
          //

          params.putString("type", "transactionAcceptedWithCard");
  
          params.putMap("payData", payData);
          params.putMap("savedCardData", savedCardData);

          sendEvent(reactContext, "didDismiss", params);
        } else if (resultCode == IntentConstants.TRANSACTION_SUCCESSFUL_PARSING_ISSUE) {
          // User finished their payment successfully and card was saved.

          // Use the static keys declared in PayResponseKeys to extract the fields you want
          // Use the static keys declared in SaveCardResponseKeys to extract the fields you want
          // ToastMaker.displayShortToast(this, "Token == " + extras.getString(SaveCardResponseKeys.TOKEN));
        } else if (resultCode == IntentConstants.USER_CANCELED_3D_SECURE_VERIFICATION) {
          // ToastMaker.displayShortToast(this, "User canceled 3-d scure verification!!");

          // Note that a payment process was attempted. You can extract the original returned values
          // Use the static keys declared in PayResponseKeys to extract the fields you want
          // ToastMaker.displayShortToast(this, extras.getString(PayResponseKeys.PENDING));
        } else if (resultCode == IntentConstants.USER_CANCELED_3D_SECURE_VERIFICATION_PARSING_ISSUE) {
          // ToastMaker.displayShortToast(this, "User canceled 3-d scure verification - Parsing Issue!!");

          // Note that a payment process was attempted.
          // User finished their payment successfully. An error occured while reading the returned JSON.
          // ToastMaker.displayShortToast(this, extras.getString(IntentConstants.RAW_PAY_RESPONSE));
        }
      }
    }
  };

  @ReactMethod
  public void presentPayVC(ReadableMap params, Promise promise) {
    try {
      Activity currentActivity = getCurrentActivity();
      Intent pay_intent = new Intent(currentActivity, PayActivity.class);

      pay_intent.putExtra(PayActivityIntentKeys.PAYMENT_KEY, params.getString("paymentKey"));
      pay_intent.putExtra(PayActivityIntentKeys.SAVE_CARD_DEFAULT, params.getBoolean("saveCardDefault"));
      pay_intent.putExtra(PayActivityIntentKeys.SHOW_SAVE_CARD, params.getBoolean("showSaveCard"));
      if(params.getString("buttonText") != null) {
        pay_intent.putExtra("PAY_BUTTON_TEXT", params.getString("buttonText"));
      }
      pay_intent.putExtra(PayActivityIntentKeys.THEME_COLOR, R.color.colorPrimary);
      pay_intent.putExtra("language", params.getBoolean("isEnglish") ? "en" : "ar");
      pay_intent.putExtra("ActionBar", false);

      ReadableMap billingData = params.getMap("billingData");
      pay_intent.putExtra(PayActivityIntentKeys.FIRST_NAME, billingData.getString("first_name"));
      pay_intent.putExtra(PayActivityIntentKeys.LAST_NAME, billingData.getString("last_name"));
      pay_intent.putExtra(PayActivityIntentKeys.BUILDING, billingData.getString("building"));
      pay_intent.putExtra(PayActivityIntentKeys.FLOOR, billingData.getString("floor"));
      pay_intent.putExtra(PayActivityIntentKeys.APARTMENT, billingData.getString("apartment"));
      pay_intent.putExtra(PayActivityIntentKeys.CITY, billingData.getString("city"));
      pay_intent.putExtra(PayActivityIntentKeys.STATE, billingData.getString("state"));
      pay_intent.putExtra(PayActivityIntentKeys.COUNTRY, billingData.getString("country"));
      pay_intent.putExtra(PayActivityIntentKeys.EMAIL, billingData.getString("email"));
      pay_intent.putExtra(PayActivityIntentKeys.PHONE_NUMBER, billingData.getString("phone_number"));
      pay_intent.putExtra(PayActivityIntentKeys.POSTAL_CODE, billingData.getString("postal_code"));

      if(params.getString("cardToken") != null && !params.getString("cardToken").isEmpty()){
        pay_intent.putExtra(PayActivityIntentKeys.TOKEN, params.getString("cardToken"));
      }

      if(params.getString("maskedCardNumber") != null && !params.getString("maskedCardNumber").isEmpty()){
        pay_intent.putExtra(PayActivityIntentKeys.MASKED_PAN_NUMBER, params.getString("maskedCardNumber"));
      }

      currentActivity.startActivityForResult(pay_intent, REQUEST_CODE);
      promise.resolve(12345);
    } catch(Exception e) {
      promise.reject("Create Event Error", e);
    }
  }

  @Override
  public String getName() {
    return "Paymob";
  }
}
