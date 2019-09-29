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

* add some kind of process for updating comrades.csv
    - manage in this repository
    - on merge to master
        - compile javascript
        - copy compiled files to website
        - commit
        - split comrades.csv into two, maintained and not
            - create a branch on wowman
            - update README
            - open a PR
* add link *back* to wowman 'other addon managers'
* add description, declare bias
* add link for submitting an update
    - link should go to wowman-comrades repo, not wowman
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

* add simplified no-javascript rendering
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
