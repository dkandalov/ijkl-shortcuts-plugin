## IJKL shortcuts plugin

This is a plugin which imposes `alt-ijkl` navigation shortcuts on currently open keymap.
The idea is that these shortcuts are more ergonomic for navigation and editing than using keyboard arrows
so you can stop using arrows in IDE editor.

These shortcuts were inspired by Vim and 
[gaming keyboard layouts](https://en.wikipedia.org/wiki/Arrow_keys#IJKL_keys).


## Shortcuts

Editor navigation:
 - `alt-i` - line up
 - `alt-j` - move to previous word
 - `alt-k` - line down
 - `alt-l` - move to next word
 - `alt-n` - move left
 - `alt-m` - move right
 - `alt-u` - move to line start
 - `alt-o` - move to line end
 - `alt-w` - page up
 - `alt-f` - page down
 - `alt-shift-ijklmnuo` - navigate with selection

Editor text modification:
 - `alt-'` - code completion
 - `alt-l` - choose lookup item and replace
 - `alt-/` - cyclic expand word
 - `cmd-l` or `ctrl-l` - complete statement
 - `alt-;` - delete next character
 - `alt-d` - delete next word
 - `alt-y` - remove line
 - `alt-e` - expand word selection
 - `alt-shift-e` - shrink word selection

Search and advanced navigation:
 - `alt-a` - highlight usages in file 
 - `alt-s` - show usages popup 
 - `alt-shift-s` - find usages 
 - `alt-h` - jump to source
 - `ctrl-cmd-b` or `ctrl-alt-b` - implementations popup
 - `alt-b` - back
 - `alt-shift-b` - forward
 - `cmd-shift-[` or `alt-8` - previous tab
 - `cmd-shift-]` or `alt-9` - next tab
 - `alt-q` - close tab
 - `shift ctrl t` - reopen closed tab

## Why these shortcuts?

Because arrows are located too far from letter and it takes too much effort to move your wrists.




## Conflicts with existing keymaps


## OSX Caveats

There are couple issues when using alt+IJKL shortcuts with built-in OSX keyboard layouts:
1. dead keys cannot be used as IDE shortcuts (e.g. `alt+i` in US layout)
2. if `alt+ik` are mapped to some character, then `Navigate to Class` action 
   handles `alt+ik` shortcuts as both navigation up/down and entering a character.
3. keys with certain output, when held down, trigger IDE action only once (e.g. `alt+i` with 'ˆ' output in US layout). 

The first two issues can be solved by adding keyboard input source which doesn't
have dead keys and doesn't output characters for `alt+ijkl` shortcuts. 
See section below.

To solve the third issue you can disable sticky keys feature by executing in shell 
`defaults write -g ApplePressAndHoldEnabled -bool false`. 


## How to install OSX input source

On IDE startup the plugin will suggest to add keyboard input sources.
In practice, this means that plugin will create `~/Library/Keyboard Layouts/ijkl-keys.bundle` directory with a bunch of files.

After input sources were installed, open OSX `System Preferences -> Keyboard -> Input Sources` and click on `+` to add input sources.

Choose `U.S. - IJKL` or `British - IJKL` input source from English category and click `Add` button.
(If you don't see these inputs, you might need to log out / log in or restart.)

Now you can switch to new input source in IDE and use `alt-ijkl` shortcuts.


## How to hide "default" input source

If you're happy with custom layout and want to hide built-in layout from input source list, 
[follow these steps](https://apple.stackexchange.com/questions/44921/how-to-remove-or-disable-a-default-keyboard-layout):
 - change the current input source to your custom keyboard layout
 - `open ~/Library/Preferences/com.apple.HIToolbox.plist` (requires XCode)
 - remove the input source or input sources you want to disable from the `AppleEnabledInputSources` dictionary; 
   if there is an `AppleDefaultAsciiInputSource` key, remove it
 - restart
 
