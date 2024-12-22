[![Build Status](https://github.com/dkandalov/ijkl-shortcuts-plugin/workflows/CI/badge.svg)](https://github.com/dkandalov/ijkl-shortcuts-plugin/actions)

## IJKL shortcuts plugin

This plugin adds `alt-ijkl` navigation shortcuts to the current keymap, tool windows, and popups. The idea is that these shortcuts are more ergonomic for navigation/editing than using keyboard arrows, so you can stop using arrows in the IDE editor.

These shortcuts are inspired by Vim and [gaming keyboard layouts](https://en.wikipedia.org/wiki/Arrow_keys#IJKL_keys).

You can see the shortcuts in action in [this talk](https://www.youtube.com/watch?v=AxxNHKCldzA).

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
 - `alt-f` - page down
 - `alt-w` - page up
 - `alt-shift-ijklmnuo` - move with selection

Editor text modification:
 - `alt-e / alt-shift-e` - expand/shrink word selection
 - `alt-;` - delete next character
 - `alt-d` - delete next word
 - `alt-y` - remove line
 - `alt-ctrl-shift-ik` - move statement up/down
 - `alt-ctrl-shift-jl` - move element left/right
 - `alt-cmd-shift-ik` (macOS) - move statement up/down
 - `alt-cmd-shift-jl` (macOS) - move element left/right
 - `alt-/` - cyclic expand word (aka hippie completion)
 - `alt-'` - code completion
 - `cmd-l` or `ctrl-l` - complete statement

Search and navigation between files:
 - `alt-a` - highlight usages in file
 - `alt-s` - show usages popup
 - `alt-shift-s` - find usages
 - `alt-h` - go to declaration or usages
 - `ctrl-cmd-b` or `ctrl-alt-b` - implementations popup
 - `alt-b` - back
 - `alt-shift-b` - forward

Tab navigation:
 - `cmd-shift-[` - previous tab
 - `cmd-shift-]` - next tab
 - `alt-q` - close tab
 - `ctrl-shift-t` - reopen closed tab


## Why editor navigation shortcuts?

Because existing key layouts are inefficient and painful to use.
See the reasons for choosing particular keys below.

### IJKL
When touch typing you would normally position your index fingers on letters `f` and `j`. This is fine for writing a lot of text sequentially. The problem is that writing/editing code is never linear and requires a lot of navigation even for simplest tasks. Navigation with standard key layouts makes you move right hand from `j` letter to arrows and back to `j`. This takes a lot of effort.

It would be great move the arrow eys into the area with letters. This is what `ijkl` mapped to `up/left/down/right` is trying to achieve.

### Alt
There are several options how to make `ijkl` work for navigation.

One option could be something like Vim command/insert modes, i.e. "command mode" in which `ijkl` keys work as arrows and "insert mode" in which all keys work as in traditional editors.

Another option is to use modifier keys to change behaviour of `ijkl` letters (or you might think about it as enabling "command mode" only when modifier key is pressed). In general, this option is easier to implement in various editors/IDEs. And among modifier keys, `alt` (the left one) was the least used in existing IDE keymaps.

### MN and move to next/previous word 
Three is a problem with arrow navigation that left/right arrows jump only one character at a time. You can use `ctrl-left/right` (or `alt-left/right` on macOS) to jump between words. This is more useful and, arguably, should be the default navigation for arrows. That's why in `alt-jl` moves caret to previous/next word. Single character navigation is still useful sometimes, so it's mapped to `alt-nm`. There is no particular reason for these letters except that they are located not too far from `ijkl`.

### UO
Moving to the start/end of line is another important part of navigation, so ideally it should be mapped to keys not far from `ijkl`. For this reason `u` and `o` seem like a great choice.
 
Moving to the start of line is `u` because it is close to `j` and essentially also means moving caret left. And `o` is mapped to the end of line because it's about moving right, similar to `l` (there is an implicit assumption here that you're writing code left to right).


### FW
The choice of `fw` letters mapped to page down/up was copied from [less](https://en.wikipedia.org/wiki/Less_(Unix)) and admittedly is somewhat arbitrary.


## MacOS Caveats

There are few issues when using alt-ijkl shortcuts with built-in macOS keyboard layouts:
1. dead keys cannot be used as IDE shortcuts (e.g. `alt-i` in US layout)
2. if `alt-ik` are mapped to some character, then `Navigate to Class` action handles `alt-ik` shortcuts as both navigation up/down and entering a character.
3. keys with certain output, when held down, trigger IDE action only once (e.g. `alt-i` with 'ˆ' output in US layout).

The first two issues can be solved by adding keyboard input source which doesn't have dead keys and doesn't output characters for `alt-ijkl` shortcuts. See section below.

To solve the third issue you can disable sticky keys feature by executing in shell `defaults write -g ApplePressAndHoldEnabled -bool false`. 


## How to install macOS input source

On IDE startup the plugin will suggest to add keyboard input sources. In practice, this means that plugin will create `~/Library/Keyboard Layouts/ijkl-keys.bundle` directory with a bunch of files.

After input sources were installed, open macOS `System Preferences -> Keyboard -> Input Sources` and click on `+` to add input sources.

Choose `U.S. - IJKL` or `British - IJKL` input source from English category and click `Add` button. (If you don't see these inputs, you might need to log out / log in or restart.)

Now you can switch to the new input source in IDE and use `alt-ijkl` shortcuts.


## How to hide "default" input source

If you're happy with custom layout and want to hide built-in layout from input source list, 
[follow these steps](https://apple.stackexchange.com/questions/44921/how-to-remove-or-disable-a-default-keyboard-layout):
 - change the current input source to your custom keyboard layout
 - `open ~/Library/Preferences/com.apple.HIToolbox.plist` (requires XCode)
 - remove the input source or input sources you want to disable from the `AppleEnabledInputSources` dictionary; 
   if there is an `AppleDefaultAsciiInputSource` key, remove it
 - restart
