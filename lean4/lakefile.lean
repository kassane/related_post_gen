import Lake
open Lake DSL

package «lean4» {
  -- add package configuration options here
}

lean_lib «Lean4» {
  -- add library configuration options here
}

@[default_target]
lean_exe «lean4» {
  root := `Main
  buildType := .release
  moreLinkArgs := #["-O3"]
}
