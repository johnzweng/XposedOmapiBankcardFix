# Xposed Module "OMAPI BankcardMobil Fix"


### What's this:

This is a module for the [Xposed Framework](http://repo.xposed.info/). You need to have the Xposed framework (which is not made by me) installed on your phone. You may need to have root to be able to install the Xposed framework (but no root is needed for this Xposed module).


### Purpose of this module:

**Please beware: This module has a very specific purpose and has probably not much use in any other situation.** It is mainly published as reference.

This module does two things:

1. **Always use "SIM1":**<br>
in a Dual-SIM phone, which has the "OMAPI" (OpenMobile API) installed, (which is also known as *SmartcardService*), it overrides the `getTerminal()` method, so that the method always returns the `SIM1` terminal, even if the calling application explicitely [requested SIM2](/app/src/main/java/at/zweng/xposed/smartcardreaderfix/OmapiFixes.java#L57) or [specified *null*](/app/src/main/java/at/zweng/xposed/smartcardreaderfix/OmapiFixes.java#L53) as the terminal name.
<br>
This workaround makes it possible to use OMAPI with apps that do not assume the presence of two USIM-slots and avoid that they request the wrong reader.
<br>
<br>

2. **Fix EVT_TRANSACTION Intent:**<br>
In an Offhost NFC transaction the Android system and all installed apps are not involved at all. The NFC communication directly goes between the NFC chip and the SIM card (over the so-called SWP connection) and never reaches the application processor of the phone. <br><br>
To still be able to display transaction results within an app, the applet in the SIM-card can issue a so-called `EVT_TRANSACTION` event (as specified in the [GSMA NFC Handset APIs Requirement Specification in 4.10 on page 12](http://www.gsma.com/digitalcommerce/wp-content/uploads/2013/12/GSMA-NFC05-NFC-Handset-APIs-Requirement-Specification-version-4-1.pdf)) which will be forwarded by the NFC chip to the application processor. There the NFC service should take care of this event and broadcasts it as an Intent to the relevant app.<br><br>
Unfortunately this mechanism is implemented often very different by different OEMs. The phone for which this module was developed sends an Intent with the action `com.nxp.action.TRANSACTION_DETECTED`. So this module hooks the method where this Intent gets sent [and replaces the action string to the GSMA standard](/app/src/main/java/at/zweng/xposed/smartcardreaderfix/OmapiFixes.java#L117) `com.gsma.services.nfc.action.TRANSACTION_EVENT`.
<br>
<br>
If the necessary data is available this module also [sets the data URI of the intent](/app/src/main/java/at/zweng/xposed/smartcardreaderfix/OmapiFixes.java#L126) to the value which is proposed in the GSMA document on page 13 as well as the two event data extra fields [`com.gsma.services.nfc.extra.AID`](/app/src/main/java/at/zweng/xposed/smartcardreaderfix/OmapiFixes.java#L134) and [`com.gsma.services.nfc.extra.DATA`](/app/src/main/java/at/zweng/xposed/smartcardreaderfix/OmapiFixes.java#L142).

<br>


### Tested on this device:

This module was developed and tested only on the following device:

- Device **LeEco LePro 3 (LEX720)**
- Firmware version: **EUI 5.8.018S** (Android 6.0.1)
- Build-ID: **WAXCNFN5801811012S**

This means, it may or may not work on other devices or even other software versions for the same device. Please use at your own risk.


### Further information:
See also the related thread on the XDA developers forum:<br>[http://forum.xda-developers.com/le-pro3/development/mod-patch-smartcardservice-sim-card-t3502369](http://forum.xda-developers.com/le-pro3/development/mod-patch-smartcardservice-sim-card-t3502369)
