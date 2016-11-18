package at.zweng.xposed.smartcardreaderfix;

import android.content.Intent;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;


/**
 * This class places two Xposed hooks in the NFC service and in the Smartcard Service
 * to make the Austrian Bankcard Mobil apps (https://www.bankomatkarte-mobil.at/)
 * working with the LeEco LePro 3 device.
 * <p>
 * Tested with LeEco LePro 3 (LEX720)
 * running EUI 5.8.018S (WAXCNFN5801811012S)
 * and the "at.psa.app.bankaustria" app (versionCode: 542, v2.3.1)
 * <p>
 * Author: johnzweng, john@zweng.at
 * Date: 17.11.2016
 */
public class OmapiFixes implements IXposedHookLoadPackage {

    // our target packages:
    private final static String OMAPI_SERVICE = "org.simalliance.openmobileapi.service";
    private final static String NFC_SERVICE = "com.android.nfc";
    // Prefix for Xposed logfile:
    private final static String LOG_PREFIX = "OMAPI_PSA_APPs_FIX: ";


    /**
     * Method hook for the "getTerminal()" method:
     * Here we take care that the Bankcard mobil apps ALWAYS get the SIM1 reader (as SIM1
     * is the only one connected to the NFC-chip) even if they request the "SIM2" reader
     * (what they actually do).
     * <p>
     * Please take care that your NFC-SIM-card is inserted in SIM1!
     */
    protected static final XC_MethodHook getTerminalHook = new de.robv.android.xposed.XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            String readerName = (String) param.args[0];
            if (readerName == null) {
                // change argument to "SIM1":
                param.args[0] = "SIM1";
                log(LOG_PREFIX + "getTerminal() --> APPLIED WORKAROUND: requested reader name was 'null' value, we changed it to 'SIM1'. :)");
            } else if ("SIM2".equalsIgnoreCase(readerName)) {
                param.args[0] = "SIM1";
                log(LOG_PREFIX + "getTerminal() --> APPLIED WORKAROUND: requested reader name was 'SIM2', we changed it to 'SIM1'. :)");
            }
        }
    };

    /**
     * Method hook for the "deliverSeIntent()" method:
     * After a transaction is done, the applet in the SIM card issues a EVT_TRANSACTION event.
     * This should result in an intent to be broadcasted with the action:
     * "com.gsma.services.nfc.action.TRANSACTION_EVENT"
     * <p>
     * See also the GSMA specifications at:
     * http://www.gsma.com/digitalcommerce/wp-content/uploads/2013/12/GSMA-NFC05-NFC-Handset-APIs-Requirement-Specification-version-4-1.pdf
     * <p>
     * Unfortunately the com.android.nfc package on the LeEco LePro 3 device
     * (/system/vendor/app/NQNfcNci/NQNfcNci.apk) instead uses the action:
     * "com.nxp.action.TRANSACTION_DETECTED" (which is not recognized by the bankcard apps)
     * <p>
     * Therefore, we simply exchange the intent action here, before the intent
     * gets broadcasted.
     * <p>
     * (We change nothing else but the action, all other extras in the intent stay as they are.)
     */
    protected static final XC_MethodHook deliverSeIntentHook = new de.robv.android.xposed.XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            //String pkg = (String) param.args[0];
            Intent seIntent = (Intent) param.args[1];
            // Just logging the Intent for debugging:
            // log(LOG_PREFIX + "  beforeHookedMethod 'deliverSeIntent':");
            // log(LOG_PREFIX + "     - pkg = '" + pkg + "'");
            // log(LOG_PREFIX + "     - seIntent: action='" + seIntent.getAction() + "', toString: " + seIntent.toString());
            // log(LOG_PREFIX + "         dataString: " + seIntent.getDataString());
            // log(LOG_PREFIX + "         getData(URI): " + seIntent.getData());
            // log(LOG_PREFIX + "         dumping Intent extras:");
            //-- Dump the extras for debugging:
            // Bundle bundle = seIntent.getExtras();
            // if (bundle != null) {
            //     Set<String> keys = bundle.keySet();
            //     Iterator<String> it = keys.iterator();
            //     while (it.hasNext()) {
            //         String key = it.next();
            //         Object val = bundle.get(key);
            //         String dump = val.toString() + "  (" + val.getClass().getCanonicalName() + ")";
            //         if (val instanceof byte[]) {
            //             dump += ": " + bytesToHex((byte[]) val);
            //         }
            //         log(LOG_PREFIX + "           [" + key + "=" + dump + "]");
            //     }
            //     log(LOG_PREFIX + "         dumping extras end");
            // }


            // In the first version I only changed the action for packages starting with "at.psa", but as
            // LeEco doesn't follow the GSMA standard here, I decided to change it for all apps:

            //if (pkg.startsWith("at.psa") && "com.nxp.action.TRANSACTION_DETECTED".equals(seIntent.getAction())) {

            if ("com.nxp.action.TRANSACTION_DETECTED".equals(seIntent.getAction())) {
                // set the new action
                seIntent.setAction("com.gsma.services.nfc.action.TRANSACTION_EVENT");
                log(LOG_PREFIX + "deliverSeIntent() --> APPLIED WORKAROUND: changed Intent action to: com.gsma.services.nfc.action.TRANSACTION_EVENT");
            }
        }
    };

    // /**
    //  * Helper method, returns String representation of byte array.
    //  * Was only used for logging the intent extras..
    //  *
    //  * @param bytes
    //  * @return
    //  */
    // private static String bytesToHex(byte[] bytes) {
    //     if (bytes == null) {
    //         return "<null>";
    //     }
    //     if (bytes.length == 0) {
    //         return "[]";
    //     }
    //     final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    //     char[] hexChars = new char[bytes.length * 2];
    //     int v;
    //     for (int j = 0; j < bytes.length; j++) {
    //         v = bytes[j] & 0xFF;
    //         hexChars[j * 2] = hexArray[v >>> 4];
    //         hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    //     }
    //     return new String(hexChars);
    // }

    /**
     * Place hooks at packages load time
     *
     * @param lpparam
     * @throws Throwable
     */
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Hook the SmartcardService:
        if (OMAPI_SERVICE.equals(lpparam.packageName)) {
            hookOmapiService(lpparam);
        }
        // and the NFC service:
        else if (NFC_SERVICE.equals(lpparam.packageName)) {
            hookNfcService(lpparam);
        }
    }

    /**
     * Hook the NFC service
     *
     * @param lpparam
     * @throws Throwable
     */
    private void hookNfcService(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        log(LOG_PREFIX + "We are in " + NFC_SERVICE + " application. Will try to place method hooks for TRANSACTION_EVENT Fix. :)");

        // This is our target:
        //
        // in class "com.android.nfc.NfcService.NxpNfcAdapterExtrasService":
        //
        //   public void deliverSeIntent(String pkg, Intent seIntent) throws RemoteException {
        //     ....
        //   }

        //
        // 1) Try to find class:
        //
        Class<?> nfcAdapterExtrasClass;
        try {
            nfcAdapterExtrasClass = findClass("com.android.nfc.NfcService.NxpNfcAdapterExtrasService",
                    lpparam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge
                    .log(LOG_PREFIX + "Could not find matching class 'NxpNfcAdapterExtrasService' for hooking. Sorry, I cannot do anything. :-(");
            // abort if class not found..
            return;
        }

        //
        // 2) Try to find method:
        //
        Method methodDeliverSeIntent = null;
        try {
            methodDeliverSeIntent = findMethodExact(nfcAdapterExtrasClass, "deliverSeIntent", String.class,
                    Intent.class);
        } catch (NoSuchMethodError nsme) {
            XposedBridge
                    .log(LOG_PREFIX + "method deliverSeIntent() was not found. :-(");
            return;
        }

        //
        // 3) and place the method hook:
        //
        XposedBridge.hookMethod(methodDeliverSeIntent, deliverSeIntentHook);
        log(LOG_PREFIX + "Success. Hooked the method: deliverSeIntent() :-)");
    }

    /**
     * Hook the OMAPI SmartcardService
     *
     * @param lpparam
     * @throws Throwable
     */
    private void hookOmapiService(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        log(LOG_PREFIX + "We are in " + OMAPI_SERVICE + " application. Will try to place method hooks for SmartcardReader Fix. :)");

        // This is our target:
        //
        // in class "org.simalliance.openmobileapi.service.SmartcardService":
        //
        //   public static ITerminal getTerminal(String reader, SmartcardError error) {
        //     .....
        //   }


        //
        // 1) Try to find class:
        //
        Class<?> smartcardServiceClass;
        try {
            smartcardServiceClass = findClass("org.simalliance.openmobileapi.service.SmartcardService",
                    lpparam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge
                    .log(LOG_PREFIX + "Could not find matching class 'SmartcardService' for hooking. Sorry, I cannot do anything. :-(");
            // abort if class not found..
            return;
        }

        //
        // 2) Try to find method:
        //
        Method methodGetTerminal = null;
        try {
            methodGetTerminal = findMethodExact(smartcardServiceClass, "getTerminal", String.class,
                    "org.simalliance.openmobileapi.service.SmartcardError");
        } catch (NoSuchMethodError nsme) {
            XposedBridge
                    .log(LOG_PREFIX + "method getTerminal() was not found. :-(");
            return;
        }

        //
        // 3) and place the method hook:
        //
        XposedBridge.hookMethod(methodGetTerminal, getTerminalHook);
        log(LOG_PREFIX + "Success. Hooked the method: getTerminal() :-)");
    }

}
