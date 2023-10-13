import Lean
import Lean.Data.Json.Parser

structure Post where
  _id: String
  title: String
  tags: Array String

structure TopPost where
  _id: String
  related: Array Post
  tags: Array String


def posting : Post := {_id := "1", title := "Foo", tags := #["x","y"]}

#check posting.tags

def lessthan (lhs: Nat)( rhs: Nat) : Bool :=
  lhs < rhs

#eval lessthan 4 3 -- false [correct: 4 > 3]

open Lean
def main : IO Unit := do
  let s ← IO.FS.readFile "../posts.json" 
  let j := (Json.parse s)
  -- IO.println j -- readfile (json parsed)