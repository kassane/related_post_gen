import Lean
import Lean.Data.Json.Basic
import Lean.Data.Json.Parser

open Lean

structure Post where
  _id: String
  title: String
  tags: Array String

structure TopPost where
  _id: String
  related: Array Post
  tags: Array String


@[inline] def posting : Post := {_id := "1", title := "Foo", tags := #["x","y"]}

-- #check posting.tags

@[inline] def lessthan (lhs: Nat)( rhs: Nat) : Bool :=
  lhs < rhs

-- #eval lessthan 4 3 -- false [correct: 4 > 3]

@[inline] def writeFile (s:String) :=
  IO.FS.writeFile "../related_posts_lean4.json" s


def main : IO Unit := do
  let startTime ← IO.monoMsNow
  let s ← IO.FS.readFile "../posts.json" 
  -- let j := (Json.parse s)


  IO.println s!"Processing time (w/o IO): {(← IO.monoMsNow) - startTime}ms\n"
  writeFile s
