# 0.1.0 release

* 'yes' and 'yes\*' need to be linked. 'yes' is 'yes' without caveats. 'yes\*' is both 'yes' and 'yes*' with caveats
* some tests, any tests
* CI
    - maybe investigate Github actions?
        - https://github.com/features/actions
* compilation with tree shaking. 
    - I want a tiny little static website. no bullshit
* update wowman README with link to picker
* replace booleans with ticks and crosses
    - preserve text label in dropdown
    - tick with caveat? 
        - "  ✓*  "
        - looks kinda shit
* 'reset' link
    - removes all selections
* change licence to AGPL

# 0.2.0 release

* group results
* determine user's platform based on user agent (if windows, select windows, etc)
    - perhaps a standard platform profile? 
        - a 'windows' profile gets unambiguous windows 'yes', 'gui'
        - a 'linux' profile gets ambiguous linux 'yes*', 'f/oss' yes, etc
