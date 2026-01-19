param(
  [Parameter(Mandatory=$true)][string]$GameId,
  [string]$Tag = "default",
  [string]$LibraryRoot = "shared_assets/bgm",
  [int]$Seconds = 16,
  [int]$SampleRate = 44100,
  [string]$AssignProject = ""
)
$ErrorActionPreference = "Stop"
$py = "python"
$argsList = @(
  "tools/assets/synth_bgm.py",
  "-GameId", $GameId,
  "-Tag", $Tag,
  "-LibraryRoot", $LibraryRoot,
  "-Seconds", "$Seconds",
  "-SampleRate", "$SampleRate"
)
if ($AssignProject -ne "") {
  $argsList += @("-AssignProject", $AssignProject)
}
& $py @argsList
