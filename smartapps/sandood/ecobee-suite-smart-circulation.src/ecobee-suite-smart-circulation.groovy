/**
 *  ecobee Suite Smart Circulation
 *
 *  Copyright 2017 Barry A. Burke
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0  
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * <snip>
 *	1.4.0  - Renamed parent Ecobee Suite Manager
 *	1.4.01 - Tweaked supportedThermostatModes handling
 *	1.4.02 - Added install warning to description
 *	1.4.03 - Optimizations for multiple simultaneous updates
 *	1.4.04 - Minor tweaks
 *	1.4.05 - Added Quiet Time Helper integration
 *	1.5.00 - Release number synchronization
 *	1.5.01 - Allow Ecobee Suite Thermostats only
 *	1.5.02 - Converted all math to BigDecimal
 *	1.5.03 - Miscellaneous bug fixes
 *	1.5.04 - Added 'circOff' and 'vacaCircOff' reservation handling (applies to quiet time only)
 *	1.6.00 - Release number synchronization
 *	1.6.01 - Fix reservation initialization error
 *	1.6.02 - REALLY fix reservations initialization error
 *	1.6.03 - Really, REALLY fix reservations initialization error
 *	1.6.10 - Converted to parent-based reservations
 *	1.6.11 - Clear reservations when disabled
 *	1.6.12 - Minor optimizations
 *	1.6.13 - Added humidity restrictor
 */
def getVersionNum() { return "1.6.13" }
private def getVersionLabel() { return "Ecobee Suite Smart Circulation Helper, version ${getVersionNum()}" }
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

definition(
	name: "ecobee Suite Smart Circulation",
	namespace: "sandood",
	author: "Barry A. Burke (storageanarchy at gmail dot com)",
	description: "INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nAdjust fan circulation time based on temperature delta between 2 or more rooms.",
	category: "Convenience",
	parent: "sandood:Ecobee Suite Manager",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
	singleInstance: false,
    pausable: true
)

preferences {
	page(name: "mainPage")
}

// Preferences Pages
def mainPage() {
	dynamicPage(name: "mainPage", title: "${getVersionLabel()}", uninstall: true, install: true) {
    	section(title: "Name for Smart Circulation Helper") {
        	label title: "Name this Helper", required: true, defaultValue: "Smart Circulation"  
        }
        
        section(title: "Select Thermostat") {
        	if(settings.tempDisable) {paragraph "WARNING: Temporarily Disabled as requested. Turn back on below to activate handler."}
            else {
        		input(name: "theThermostat", type:"device.ecobeeSuiteThermostat", title: "Use which Ecobee Thermostat", required: true, multiple: false, 
                submitOnChange: true)
            }
		}
        
        if (!settings.tempDisable) {
        	section(title: "Select Indoor Temperature Sensors") {
            	input(name: "theSensors", title: "Use which indoor temperature sensor(s)", type: "capability.temperatureMeasurement", required: true, multiple: true, submitOnChange: true)
			}
        
       		section(title: "Fan On Time Automation Configuration") {
        		paragraph("Increase Circulation time (min/hr) when the difference between the maximum and the minimum temperature reading of the above sensors is more than this.")
            	input(name: "deltaTemp", type: "enum", title: "Select temperature delta", required: true, defaultValue: "2.0", multiple:false, options:["1.0", "1.5", "2.0", "2.5", "3.0", "4.0", "5.0", "7.5", "10.0"])
            	paragraph("Minimum Circulation time (min/hr). Includes heating, cooling and fan only minutes.")
            	input(name: "minFanOnTime", type: "number", title: "Set minimum fan on min/hr (0-${settings.maxFanOnTime!=null?settings.maxFanOnTime:55})", required: true, defaultValue: "5", description: "5", range: "0..${settings.maxFanOnTime!=null?settings.maxFanOnTime:55}", submitOnChange: true)
            	paragraph("Maximum Circulation time (min/hr).")
            	input(name: "maxFanOnTime", type: "number", title: "Set maximum fan on min/hr (${settings.minFanOnTime!=null?settings.minFanOnTime:5}-55)", required: true, defaultValue: "55", description: "55", range: "${settings.minFanOnTime!=null?settings.minFanOnTime:5}..55", submitOnChange: true)
            	paragraph("Adjust Circulation time (min/hr) by this many minutes each adjustment.")
            	input(name: "fanOnTimeDelta", type: "number", title: "Minutes per adjustment (1-20)", required: true, defaultValue: "5", description: "5", range: "1..20")
            	paragraph("Minimum number of minutes between adjustments.")
            	input(name: "fanAdjustMinutes", type: "number", title: "Time adjustment frequency in minutes (5-60)", required: true, defaultValue: "10", description: "15", range: "5..60")
        	}
            
            section(title: "Indoors/Outdoors Temperature Delta") {
            	paragraph("To apply above adjustments based on inside/outside temperature difference, first select an outside temperature source (indoor temperature will be the average of the sensors selected above).")
                input(name: "outdoorSensor", title: "Use which outdoor temperature sensor", type: "capability.temperatureMeasurement", required: false, multiple: false, submitOnChange: true)
                if (outdoorSensor) {
                	paragraph("Select the indoor/outdoor delta temperature range for which you want to apply the above automated adjustments.")
                    input(name: "adjRange", type: "enum", title: "Adjust fan on times only when outside delta is in this range", multiple: false, required: true, 
							options: ["More than 10 degrees warmer", "5 to 10 degrees warmer", "0 to 4.9 degrees warmer", "-4.9 to -0.1 degrees cooler",
                            			"-10 to -5 degrees cooler", "More than 10 degrees cooler"], submitOnChange: true)
                }
            }
       
        	section(title: "Vacation Hold Override") {
        		paragraph("The thermostat's Circulation setting is overridden when a Vacation is in effect. If you would like to automate the Circulation time during a Vacation hold, enable this setting.")
            	input(name: "vacationOverride", type: "bool", title: "Override fan during Vacation hold?", defaulValue: false)
        	}
       
			section(title: "Enable only for specific modes or programs?") {
        		paragraph("Circulation time (min/hr) is only adjusted while in these modes *OR* programs. The time will remain at the last setting while in other modes. If you want different circulation times for other modes or programs, create multiple Smart Circulation handlers.")
            	input(name: "theModes", type: "mode", title: "Only when the Location Mode is", multiple: true, required: false)
                input(name: "statModes", type: "enum", title: "Only when the ${settings.theThermostat!=null?settings.theThermostat:'thermostat'}'s Mode is", multiple: true, required: false, options: getThermostatModesList())
            	input(name: "thePrograms", type: "enum", title: "Only when the ${settings.theThermostat!=null?settings.theThermostat:'thermostat'}'s Program is", multiple: true, required: false, options: getProgramsList())
        	}
            
            section(title: "Enable only when relative humidity is high?") {
            	paragraph("Circulation time (min/hr) is adjusted only when the relative humidity is higher than a specified value")
                input(name: "theHumidistat", type: "capability.relativeHumidityMeasurement", title: "Use this humidity sensor (blank to disable)", multiple: false, required: false, submitOnChange: true)
                if (settings.theHumidistat) {
                	input( name: "highHumidity", type: "number", title: "Adjust circulation only when ${settings.theHumidistat.displayName}'s Relative Humidity is higher than:", range: "0..100", required: true)
                }
            }
            
            section(title: "'Quiet Time' Integration") {
            	paragraph("You can configure this Helper to integrate with one or more instances of the Ecobee Suite Quiet Time Helper: This helper will stop updating circulation when one or more Quiet Time switch(es) are enabled.")
            	input(name: "quietSwitches", type: "capability.switch", title: "Select Quiet Time control switch(es)", multiple: true, required: false, submitOnChange: true)
                if (settings.quietSwitches) {
                	paragraph("All selected Quiet Time switches must use the same state to turn on Quiet Time.")
                	input(name: "qtOn", type: "enum", title: "Disable circulation when any of these Quiet Switches is:", defaultValue: 'on', required: true, multiple: false, options: ["on","off"])
                }
            }
		}
        
		section(title: "Temporarily Disable?") {
        	input(name: "tempDisable", title: "Temporarily Disable Handler? ", type: "bool", required: false, description: "", submitOnChange: true)                
        }
        
        section (getVersionLabel())
    }
}

// Main functions
def installed() {
	LOG("installed() entered", 4, "", 'trace')
    
    // initialize the min/max trackers...plan to use these to optimize the decrease cycles
    atomicState.maxMax = 0.0
    atomicState.minMin = 100.0
    atomicState.maxDelta = 0.0
    atomicState.minDelta = 100.0    
	initialize()  
}

def uninstalled() {
	clearReservations()
}
def clearReservations() {
	cancelReservation( getDeviceId(theThermostat?.deviceNetworkId), 'circOff' )
    cancelReservation( getDeviceId(theThermostat?.deviceNetworkId), 'vacaCircOff' )
}
def updated() {
	LOG("updated() entered", 4, "", 'trace')
	unsubscribe()
    unschedule()
    initialize()
}

def getProgramsList() {
	if (theThermostat) {
    	def programs = theThermostat.currentValue('programsList')
        if (programs) { 
        	return new JsonSlurper().parseText(programs)
        }
    }
    return ["Away","Home","Sleep"]
}

def getThermostatModesList() {
	def statModes = ["off","heat","cool","auto","auxHeatOnly"]
    if (settings.theThermostat) {
    	def tempModes = theThermostat.currentValue('supportedThermostatModes')
        if (tempModes) statModes = tempModes[1..-2].tokenize(", ")
    }
    return statModes
}

def initialize() {
	LOG("${getVersionLabel()}\nInitializing...", 3, "", 'info')
	atomicState.amIRunning = null // Now using runIn to collapse multiple calls into single calcTemps()
    def mode = location.mode
    
	// Now, just exit if we are disabled...
	if(tempDisable == true) {
    	clearReservations()
    	LOG("temporarily disabled as per request.", 2, null, "warn")
    	return true
    }
    
    // Initialize as if we haven't checked in more than fanAdjustMinutes
    atomicState.lastAdjustmentTime = now() // - (60001 * fanAdjustMinutes.toLong()).toLong() // make sure we run on next deltaHandler event    
    subscribe(theThermostat, "thermostatOperatingState", modeOrProgramHandler)		// so we can see when the fan runs
    if (thePrograms) subscribe(theThermostat, "currentProgram", modeOrProgramHandler)
    // subscribe(theThermostat, "thermostatHold", modeOrProgramHandler)
    subscribe(location, "routineExecuted", modeOrProgramHandler)    
    if (theModes) subscribe(location, "mode", modeOrProgramHandler)
    if (statModes) subscribe(theThermostat, "thermostatMode", modeOrProgramHandler)
    
    if (settings.quietSwitches) {
    	subscribe(quietSwitches, "switch.${qtOn}", quietOnHandler)
        def qtOff = settings.qtOn == 'on' ? 'off' : 'on'
        subscribe(quietSwitches, "switch.${off}", quietOffHandler)
        atomicState.quietNow = (settings.quietSwitches.currentSwitch.contains(settings.qtOn)) ? true : false
    } else {
    	atomicState.quietNow = false
    }
    
    subscribe(theSensors, "temperature", deltaHandler)
    if (outdoorSensor) {
    	if (outdoorSensor.id == theThermostat.id) {
        	LOG("Using Ecobee-supplied external weatherTemperature from ${theThermostat.displayName}.",1,null,'info')
        	subscribe(theThermostat, "weatherTemperature", deltaHandler)
        } else {
        	LOG("Using external temperature from ${outdoorSensor.displayName}.",1,null,'info')
        	subscribe(outdoorSensor, "temperature", deltaHandler)
        }
    }

	def fanOnTime = theThermostat.currentValue('fanMinOnTime')
    int currentOnTime = fanOnTime?.isNumber() ? fanOnTime.toInteger() : 0
    boolean vacationHold = (theThermostat.currentValue("currentProgram") == "Vacation")
    
	// log.debug "settings ${theModes}, location ${location.mode}, programs ${thePrograms} & ${programsList}, thermostat ${theThermostat.currentValue('currentProgram')}, currentOnTime ${currentOnTime}, quietSwitch ${quietSwitches.displayName}, quietState ${quietState}"
   
	// Allow adjustments if Location Mode or Thermostat Program or Thermostat Mode is currently as configured
    // Also allow if none are configured
    boolean isOK = true
    if (theModes || thePrograms  || statModes) {
    	isOK = (theModes && theModes.contains(location.mode)) ? true : 
        			((thePrograms && thePrograms.contains(theThermostat.currentValue('currentProgram'))) ? true : 
                    	((statModes && statModes.contains(theThermostat.currentValue('thermostatMode'))) ? true : false))
        if (!isOK) LOG("Not in specified Mode or Program, not adjusting", 3, null, "info")
    }
    
    // Check the humidity?
    if (isOK && settings.theHumidistat) {
    	if (settings.theHumidistat.currentHumidity.toInteger() <= settings.highHumidity) {
        	isOK == false
            LOG("Relative Humidity at ${settings.theHumidistat.displayName} is only ${settings.theHumidistat.currentHumidity}% (${settings.highHumidity}% set), not adjusting", 3, null, "info")
        }
    }
    
    // Quiet Time?
    if (isOK ){
    	isOK = settings.quietSwitches ? (atomicState.quietNow != true) : true
        if (!isOK) LOG("Quiet time active, not adjusting", 3, null, "info")
    }
    atomicState.isOK = isOK
    
    def tid = getDeviceId(theThermostat.deviceNetworkId)
    if (isOK) {	
		if (currentOnTime < settings.minFanOnTime) {
    		if (vacationHold && settings.vacationOverride) {
            	cancelReservation( tid, 'vacaCircOff')
        		theThermostat.setVacationFanMinOnTime(settings.minFanOnTime)
            	currentOnTime = settings.minFanOnTime
                atomicState.lastAdjustmentTime = now() 
        	} else if (!vacationHold) {
            	cancelReservation( tid, 'circOff')
    			theThermostat.setFanMinOnTime(settings.minFanOnTime)
            	currentOnTime = settings.minFanOnTime
                atomicState.lastAdjustmentTime = now() 
        	}
    	} else if (currentOnTime > settings.maxFanOnTime) {
    		if (vacationHold && settings.vacationOverride) {
            	cancelReservation( tid, 'vacaCircOff')
        		theThermostat.setVacationFanMinOnTime(settings.maxFanOnTime)
        		currentOnTime = settings.maxFanOnTime
                atomicState.lastAdjustmentTime = now() 
        	} else if (!vacationHold) {
            	cancelReservation( tid, 'circOff')
    			theThermostat.setFanMinOnTime(settings.maxFanOnTime)
        		currentOnTime = settings.maxFanOnTime
                atomicState.lastAdjustmentTime = now() 
        	}
    	} else {
        	atomicState.fanSinceLastAdjustment = true
			deltaHandler()
            currentOnTime = -1
        }
    } else if (atomicState.quietNow) {
    	if (currentOnTime != 0) {
    		if (vacationHold && settings.vacationOverride) {
            	makeReservation(tid, 'vacaCircOff')
        		theThermostat.setVacationFanMinOnTime(0)
        	} else if (!vacationHold) {
                makeReservation(tid, 'circOff')
                theThermostat.setFanMinOnTime(0)
        	}
        }
    }
    if (currentOnTime > -1) {
    	def vaca = vacationHold ? " is in Vacation mode, " : " "    
    	LOG("thermostat ${theThermostat}${vaca}circulation time is now ${currentOnTime} min/hr",2,"",'info')
    }
    LOG("Initialization complete", 4, "", 'trace')
}

def quietOnHandler(evt) {
	LOG("Quiet Time switch ${evt.device.displayName} turned ${evt.value}", 3, null, 'info')
	if (!atomicState.quietNow) {
    	atomicState.quietNow = true
        Integer currentOnTime = theThermostat.currentValue('fanMinOnTime').isNumber() ? theThermostat.currentValue('fanMinOnTime').toInteger() : 0	
        atomicState.quietOnTime = currentOnTime
        LOG("Quiet Time enabled, ${app.name} will stop updating circulation time", 3, null, 'info')
        // NOTE: Quiet time will actually pull the circOff reservation and set circulation time to 0
    } else {
    	LOG('Quiet Time already enabled', 3, null, 'info')
    }
}

def quietOffHandler(evt) {
	LOG("Quiet Time switch ${evt.device.displayName} turned ${evt.value}", 3, null, 'info')
    // NOTE: Quiet time will release its circOff reservation and set circulation time to whatever it was
    if (atomicState.quietNow) {
    	if (!settings.quietSwitches.currentSwitch.contains(settings.qtOn)) {
	    	// All the switches are "off"
            atomicState.quietNow = false
            LOG("Quiet Time disabled, ${app.name} will resume circulation time updates", 3, null, 'info')
            modeOrProgramHandler(null)
        } else {
        	def qtOff = settings.qtOn == 'on' ? 'off' : 'on'
        	LOG("All Quiet Time switches are not ${qtOff}, Quiet Time continues", 3, null, 'info')
        }
    } else {
    	LOG("Weird, ${app.name} is not in Quiet Time", 1, null, 'warn')
    }
}

def modeOrProgramHandler(evt=null) {
	// Just exit if we are disabled...
	if(settings.tempDisable == true) {
    	LOG("${app.name} temporarily disabled as per request.", 2, null, "warn")
    	return true
    }
    
	// Allow adjustments if location.mode OR thermostat.currentProgram match configuration settings
    boolean isOK = true
    if (theModes || thePrograms  || statModes) {
    	isOK = (theModes && theModes.contains(location.mode)) ? true : 
        			((thePrograms && thePrograms.contains(theThermostat.currentValue('currentProgram'))) ? true : 
                    	((statModes && statModes.contains(theThermostat.currentValue('thermostatMode'))) ? true : false))
        if (!isOK) LOG("Not in specified Mode or Program, not adjusting", 3, null, "info")
    }
    
    // Check the humidity?
    if (isOK && settings.theHumidistat) {
    	if (settings.theHumidistat.currentHumidity.toInteger() <= settings.highHumidity) {
        	isOK == false
            LOG("Relative Humidity at ${settings.theHumidistat.displayName} is only ${settings.theHumidistat.currentHumidity}% (${settings.highHumidity}% set), not adjusting", 3, null, "info")
        }
    }
    
    // Quiet Time?
    if (isOK ){
    	isOK = settings.quietSwitches ? (atomicState.quietNow != true) : true
        if (!isOK) LOG("Quiet time active, not adjusting", 3, null, "info")
    }
    atomicState.isOK = isOK
    
    if (evt && (evt.name == "thermostatOperatingState") && !atomicState.fanSinceLastAdjustment) {
    	if ((evt.value != 'idle') && (!evt.value.contains('ending'))) atomicState.fanSinceLastAdjustment = true // [fan only, heating, cooling] but not [idle, pending heat, pending cool]
    }
	deltaHandler(evt)
}

def deltaHandler(evt=null) {
	// Just exit if we are disabled...
	if(settings.tempDisable == true) {
    	LOG("temporarily disabled as per request.", 2, null, "warn")
    	return true
    }

    // Make sure it is OK for us to e changing circulation times
	if (!atomicState.isOK) return
    
    String currentProgram = theThermostat.currentValue('currentProgram')
    boolean vacationHold = (currentProgram && (currentProgram == 'Vacation'))
	if (vacationHold && !settings.vacationOverride) {
    	LOG("${theThermostat} is in Vacation mode, but not configured to override Vacation fanMinOnTime, returning", 3, "", 'warn')
        return
    }
    def tid = getDeviceId(theThermostat.deviceNetworkId)
    if (!vacationHold) {
        if (anyReservations(tid, 'circOff') && (theThermostat.currentValue('fanMinOnTime').toInteger() == 0)) {
            // Looks like somebody else has turned off circulation
            if (!haveReservation(tid, 'circOff')) {		// is it me?
                // Not me, so we can't be changing circulation
				return
            }
        }
    } else {
    	if (anyReservations(tid, 'vacaCircOff') && (theThermostat.currentValue('fanMinOnTime').toInteger() == 0)) {
            // Looks like somebody else has turned off circulation
            if (!haveReservation(tid, 'vacaCircOff')) {		// is it me?
                // Not me, so we can't be changing circulation
				return
            }
        }
    }

	if (evt) {
    	if (evt.name == 'currentProgram') {
        	LOG("Thermostat Program changed to my Program (${evt.value})",3,null,'info')
        } else if (evt.name == 'mode') {
        	LOG("Location Mode changed to my Mode (${evt.value})",3,null,'info')
        } else {
        	LOG("Called with ${evt.device} ${evt.name} ${evt.value}",3,null,'trace')
        }
        if (settings.minFanOnTime == settings.maxFanOnTime) {
        	if (theThermostat.currentValue('fanMinOnTime')?.toInteger() == settings.minFanOnTime.toInteger()) {
    			LOG('Configured min==max==fanMinOnTime, nothing to do, skipping...',2,null,'info')
        		return // nothing to do
            } else {
                LOG("Configured min==max, setting fanMinOnTime(${settings.minFanOnTime})",2,null,'info')
                if (vacationHold && settings.vacationOverride) {
                	cancelReservation( tid, 'vacaCircOff')
        			theThermostat.setVacationFanMinOnTime(settings.fanMinOnTIme)
        		} else if (!vacationHold) {
                	cancelReservation( tid, 'circOff')
    				theThermostat.setFanMinOnTime(settings.fanMinOnTime)
        		}
                return
            }
    	}
    } else {
    	LOG("Called directly", 4, "", 'trace')
    }
	runIn(2,'calcTemps',[overwrite: true])
}

def calcTemps() {
	LOG('Calculating temperatures...', 3, null, 'info')
        
    // Makes no sense to change fanMinOnTime while heating or cooling is running - take action ONLY on events while idle or fan is running
    def statState = theThermostat.currentValue("thermostatOperatingState")
    if (statState && (statState.contains('ea') || statState.contains('oo'))) {
    	LOG("${theThermostat} is ${statState}, no adjustments made", 4, "", 'info' )
        return
    }
    
  	// Check if it has been long enough since the last change to make another...
    if (atomicState.lastAdjustmentTime) {
        def timeNow = now()
        def minutesLeft = fanAdjustMinutes - ((timeNow - atomicState.lastAdjustmentTime) / 60000).toInteger()
        if (minutesLeft >0) {
            LOG("Not time to adjust yet - ${minutesLeft} minutes left",4,'','info')
            return
		}
	}
    atomicState.lastCheckTime = now() 
    
    // parse temps - ecobee sensors can return "unknown", others may return
    def temps = []
    def total = 0.0
    def i=0
    theSensors.each {
    	def temp = it.currentValue("temperature")
    	if (temp && temp.isNumber() && (temp > 0)) {
        	temps += [temp]	// we want to deal with valid inside temperatures only
            total = total + temp
            i = i + 1
        }
    }
    def avg = 0.0
    if (i > 1) {
    	avg = roundIt((total / i.toBigDecimal()), 2) 
	    LOG("Current temperature readings: ${temps}, average is ${String.format("%.2f",avg)}°", 4, "", 'trace')
    } else {
    	LOG("Only recieved ${temps.size()} valid temperature readings, skipping...",3,"",'warn')
        return 
    }
    
    // Skip if the in/out delta doesn't match our settings
    if (outdoorSensor) {
    	def outTemp = null
        if (outdoorSensor.id == theThermostat.id) {
        	outTemp = theThermostat.currentValue("weatherTemperature")
            LOG("Using ${theThermostat.displayName}'s weatherTemperature (${outTemp}°)",4,null,"info")
        } else {
        	outTemp = outdoorSensor.currentValue("temperature")
            LOG("Using ${outdoorSensor.displayName}'s temperature (${outTemp}°)",4,null,"info")
        }
        def inoutDelta = null
        if (outTemp && outTemp.isNumber()) {
        	inoutDelta = roundIt((outTemp - avg), 2)
        }
        if (inoutDelta == null) {
        	LOG("Invalid outdoor temperature, skipping...",1,"","warn")
        	return
        }
        LOG("Outside temperature is currently ${outTemp}°, inside temperature average is ${String.format("%.2f",avg)}°",4,null,'trace')
        // More than 10 degrees warmer", "5 to 10 degrees warmer", "0 to 4.9 degrees warmer", "-4.9 to -0.1 degrees cooler", -10 to -5 degrees cooler", "More than 10 degrees cooler"
    	def inRange = false
        if (adjRange.endsWith('warmer')) {
        	if (adjRange.startsWith('M')) {
            	if (inoutDelta > 10.0) { inRange = true }
            } else if (adjRange.startsWith('5')) {
            	if ((inoutDelta <= 10.0) && (inoutDelta >= 5.0)) { inRange = true }
            } else { // 0-4.9
            	if ((inoutDelta < 5.0) && (inoutDelta >= 0.0)) { inRange = true }
            }
        } else {
        	if (adjRange.startsWith('M')) {
            	if (inoutDelta < -10.0) { inRange = true }
            } else if (adjRange.startsWith('-1')) {
            	if ((inoutDelta <= -5.0) && (inoutDelta >= -10.0)) { inRange = true }
            } else { // -4.9 -0.1
            	if ((inoutDelta > -5.0) && (inoutDelta < 0.0)) { inRange = true }
            }
        }
        if (!inRange) {
        	LOG("In/Out temperature delta (${inoutDelta}°) not in range (${adjRange}), skipping...",4,"","trace")
            return
        } else {
        	LOG("In/Out temperature delta (${inoutDelta}°) is in range (${adjRange}), adjusting...",4,"","trace")
        }
    }
    
    def min = roundIt(temps.min(), 2)
	def max = roundIt(temps.max(), 2)
	def delta = roundIt((max - min), 2)
    
    atomicState.maxMax = atomicState.maxMax > max ? roundIt(atomicState.maxMax, 2) : max 
    atomicState.minMin = atomicState.minMin < min ? roundIt(atomicState.minMin, 2) : min
    atomicState.maxDelta = atomicState.maxDelta > delta ? roundIt(atomicState.maxDelta, 2) : delta 
    atomicState.minDelta = atomicState.minDelta < delta ? roundIt(atomicState.minDelta, 2) : delta
    
    def currentOnTime
    if (atomicState.quietOnTime?.isNumber()) {
    	// pick up where we left off at the start of Quiet Time
    	currentOnTime = roundIt(atomicState.quietOnTime, 0)
        atomicState.quietOnTime = null
    } else {
    	currentOnTime = theThermostat.currentValue('fanMinOnTime').toInteger() ?: 0	// EcobeeSuite Manager will populate this with Vacation.fanMinOnTime if necessary
	}
    def newOnTime = roundIt(currentOnTime, 0)
	def tid = getDeviceId(theThermostat.deviceNetworkId)
	if (delta >= deltaTemp.toBigDecimal()) {			// need to increase recirculation (fanMinOnTime)
		newOnTime = roundIt(currentOnTime + fanOnTimeDelta, 0)
		if (newOnTime > settings.maxFanOnTime) {
			newOnTime = settings.maxFanOnTime
		}
		if (currentOnTime != newOnTime) {
			LOG("Temperature delta is ${String.format("%.2f",delta)}°/${String.format("%.2f",deltaTemp.toBigDecimal())}°, increasing circulation time for ${theThermostat} to ${newOnTime} min/hr",3,"",'info')
			if (vacationHold) {
            	cancelReservation( tid, 'vacaCircOff')
            	theThermostat.setVacationFanMinOnTime(newOnTime)
            } else {
            	LOG("deltaHandler: calling setFanMinOnTime(${newOnTime})",3,null,'info')
                cancelReservation( tid, 'circOff')
            	theThermostat.setFanMinOnTime(newOnTime)
            }
            atomicState.fanSinceLastAdjustment = false
			atomicState.lastAdjustmentTime = now()
            return
		}
	} else {
        // Double target = (getTemperatureScale() == "C") ? 0.55 : 1.0
        //atomicState.target = target
        // if (target > deltaTemp.toDouble()) target = (deltaTemp.toDouble() * 0.66667).round(2)	// arbitrary - we have to be less than deltaTemp
    	// if (delta <= target) {			// start adjusting back downwards once we get within 1F or .5556C
			newOnTime = roundIt(currentOnTime - fanOnTimeDelta, 0)
			if (newOnTime < settings.minFanOnTime) {
				newOnTime = settings.minFanOnTime
			}
            if (currentOnTime != newOnTime) {
           		LOG("Temperature delta is ${String.format("%.2f",delta)}°/${String.format("%.2f",deltaTemp.toBigDecimal())}°, decreasing circulation time for ${theThermostat} to ${newOnTime} min/hr",3,"",'info')
				if (vacationHold) {
                	LOG("Calling setVacationFanMinOnTime(${newOnTime})",3,null,'info')
                    cancelReservation( tid, 'vacaCircOff')
                	theThermostat.setVacationFanMinOnTime(newOnTime)
                } else {
                	LOG("Calling setFanMinOnTime(${newOnTime})",3,null,'info')
                    cancelReservation( tid, 'circOff')
                	theThermostat.setFanMinOnTime(newOnTime)
                }
                atomicState.fanSinceLastAdjustment = false
				atomicState.lastAdjustmentTime = now()
                return
            }
		// }
	}
	LOG("No adjustment made",4,"",'info')
}

// Reservation Management Functions - Now implemented in Ecobee Suite Manager
void makeReservation(tid, String type='modeOff' ) {
	parent.makeReservation( tid, app.id, type )
}
// Cancel my reservation
void cancelReservation(tid, String type='modeOff') {
	log.debug "cancel ${tid}, ${type}"
	parent.cancelReservation( tid, app.id, type )
}
// Do I have a reservation?
Boolean haveReservation(tid, String type='modeOff') {
	return parent.haveReservation( tid, app.id, type )
}
// Do any Apps have reservations?
Boolean anyReservations(tid, String type='modeOff') {
	return parent.anyReservations( tid, type )
}
// How many apps have reservations?
Integer countReservations(tid, String type='modeOff') {
	return parent.countReservations( tid, type )
}
// Get the list of app IDs that have reservations
List getReservations(tid, String type='modeOff') {
	return parent.getReservations( tid, type )
}
// Get the list of app Names that have reservations
List getGuestList(tid, String type='modeOff') {
	return parent.getGuestList( tid, type )
}

// Helper Functions
private def getDeviceId(networkId) {
	// def deviceId = networkId.split(/\./).last()	
    // LOG("getDeviceId() returning ${deviceId}", 4, null, 'trace')
    // return deviceId
    return networkId.split(/\./).last()
}

private roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
private roundIt( BigDecimal value, decimals=0 ) {
	return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	message = "${app.label} ${message}"
	if (logType == null) logType = 'debug'
	parent.LOG(message, level, null, logType, event, displayEvent)
    log."${logType}" message
}
