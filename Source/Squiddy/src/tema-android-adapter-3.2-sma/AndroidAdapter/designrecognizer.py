#
# Copyright 2014 Shahriyar Amini
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

__author__ = 'shahriyar'
__copyright__ = 'Copyright 2014, Shahriyar Amini'

import AndroidAdapter.utils as utils
import AndroidAdapter.constants as const

LOGIN_PARADIGM_TERMS_STRING = "password,passwd,username,user name,login,log in,new account,create,sign up,signup,sign in,signin,forgot password,forgot username"
LOGIN_PARADIGM_TERMS = LOGIN_PARADIGM_TERMS_STRING.split(',')

RATE_APP_PARADIGM_TERMS_STRING = "rating,star,5-star"
RATE_APP_PARADIGM_TERMS = RATE_APP_PARADIGM_TERMS_STRING.split(',')

SHARE_APP_PARADIGM_TERMS_STRING = "share app with friends,share app,share this app"
SHARE_APP_PARADIGM_TERMS = SHARE_APP_PARADIGM_TERMS_STRING.split(',')

OPEN_WITH_TERMS_STRING = "complete action,complete action using"
OPEN_WITH_TERMS = OPEN_WITH_TERMS_STRING.split(',')

NEGATIVE_RESPONSE_TERMS_STRING = "not now,no,not,cancel,later,don't ask,do not ask,remind"
NEGATIVE_RESPONSE_TERMS = NEGATIVE_RESPONSE_TERMS_STRING.split(',')

def recognizeLoginComponent(text):
    return utils.matchText(text, LOGIN_PARADIGM_TERMS)

def recognizeRateAppComponent(text):
    return utils.matchText(text, RATE_APP_PARADIGM_TERMS)

def recognizeShareAppComponent(text):
    return utils.matchText(text, SHARE_APP_PARADIGM_TERMS)

def recognizeOpenWithComponent(text):
    return utils.matchText(text, OPEN_WITH_TERMS)

def recognizeNegativeResponseComponent(text):
    return utils.matchText(text, NEGATIVE_RESPONSE_TERMS)

def recognizeComponent(text):
    retVal = 0
    
    if recognizeLoginComponent(text):
        retVal |= const.COMPONENT_LOGIN
        
    if recognizeShareAppComponent(text):
        retVal |= const.COMPONENT_SHARE_APP
    
    if recognizeRateAppComponent(text):
        retVal |= const.COMPONENT_RATE_APP
    
    if recognizeOpenWithComponent(text):
        retVal |= const.COMPONENT_OPEN_WITH
        
    if recognizeNegativeResponseComponent(text):
        retVal |= const.COMPONENT_NEGATIVE_RESPONSE
    
    if retVal == 0:
        retVal = const.COMPONENT_UNKNOWN
        
    return retVal

def compareTag(tag, compValue):
    if tag is None or compValue is None:
            return False
        
    return ((tag & compValue) > 0)
    
    

