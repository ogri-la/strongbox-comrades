# 0.1.0 release

## done

* 'yes' and 'yes\*' need to be linked. 'yes' is 'yes' without caveats. 'yes\*' is both 'yes' and 'yes*' with caveats
* handle warnings
* some tests, any tests
* compilation with tree shaking. 
    - I want a tiny little static website. no bullshit
        - I can get a 256KB minified .js file that includes the contents of comrades.csv
            - see README for compilation instructions
            - good enough

## todo

* options available in dropdowns should become more limited as filters narrow
* CI
    - maybe investigate Github actions?
        - https://github.com/features/actions
* 'reset' link
    - removes all selections
* change licence to AGPL
* last, update wowman README with link to picker

# 0.2.0 release

## todo

* replace booleans with ticks and crosses
    - preserve text label in dropdown
    - tick with caveat? 
        - "  ✓*  "
        - looks kinda shit
* group results
* determine user's platform based on user agent (if windows, select windows, etc)
    - perhaps a standard platform profile? 
        - a 'windows' profile gets unambiguous windows 'yes', 'gui'
        - a 'linux' profile gets ambiguous linux 'yes*', 'f/oss' yes, etc
