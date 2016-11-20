# Changelog

## Version 1.1:
20.11.2016

- Added further details specified in GSMA requirements specifications. Should now be compatible with even more NFC/OMAPI apps:
	- **EVT_TRANSACTION**: also set the extra fields in the Intent to GSMA standard names:
		- `com.gsma.services.nfc.extra.AID`
		- `com.gsma.services.nfc.extra.DATA`
		- Also set the data URI of the intent to GSMA standard: `nfc://secure:0/<SEName>/<AID>`
I

## Version 1.0:

- first published version
- **Reader API**: forces that for all Open Mobile API rquests SIM1 will be used, even if the app requests SIM2
- **EVT_TRANSACTION**: changes the action string of the Intent which will be broadcasted after offhost transactions to the GSMA standard `com.nxp.action.TRANSACTION_DETECTED `

