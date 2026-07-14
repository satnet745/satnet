$ErrorActionPreference = 'Stop'
Set-Location 'C:\Users\Test\AndroidStudioProjects\satphone'

function Invoke-Git {
  param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Args)
  & git @Args
  if ($LASTEXITCODE -ne 0) {
    throw "git $($Args -join ' ') failed with exit code $LASTEXITCODE"
  }
}

$env:GIT_AUTHOR_NAME = 'naknet'
$env:GIT_AUTHOR_EMAIL = 'naknet@proton.me'
$env:GIT_COMMITTER_NAME = 'naknet'
$env:GIT_COMMITTER_EMAIL = 'naknet@proton.me'

Invoke-Git checkout --detach e8941bd6

Invoke-Git cherry-pick --no-commit ee5da1d0
Invoke-Git commit --reuse-message=ee5da1d0 --reset-author
$devSha = (git rev-parse HEAD).Trim()

Invoke-Git cherry-pick --no-commit a9ad1cc0
Invoke-Git commit --reuse-message=a9ad1cc0 --reset-author

Invoke-Git cherry-pick --no-commit bc282b77

$gitignore = '.gitignore'
$gitignoreContent = Get-Content $gitignore
if ($gitignoreContent -notcontains '/.idea/copilot*.xml') {
  $updatedGitignore = foreach ($line in $gitignoreContent) {
    $line
    if ($line -eq '/.idea/workspace.xml') {
      '/.idea/copilot*.xml'
    }
  }
  Set-Content -Path $gitignore -Value $updatedGitignore -Encoding UTF8
}

$checklistPath = 'SATNET_GLOBAL_CHECKLIST.md'
$checklistContent = Get-Content $checklistPath | Where-Object { $_ -notmatch '^\*\*Created By:\*\* GitHub Copilot' }
Set-Content -Path $checklistPath -Value $checklistContent -Encoding UTF8

Invoke-Git rm -f --ignore-unmatch .idea/copilot.data.migration.agent.xml .idea/copilot.data.migration.ask.xml .idea/copilot.data.migration.ask2agent.xml .idea/copilot.data.migration.edit.xml
Invoke-Git add .gitignore SATNET_GLOBAL_CHECKLIST.md
Invoke-Git commit --reuse-message=bc282b77 --reset-author

Invoke-Git cherry-pick --no-commit 49f842e2
Invoke-Git commit --reuse-message=49f842e2 --reset-author
$mainSha = (git rev-parse HEAD).Trim()

Invoke-Git branch -f development $devSha
Invoke-Git branch -f main $mainSha
Invoke-Git checkout main

Write-Host "DEVELOPMENT=$devSha"
Write-Host "MAIN=$mainSha"
git --no-pager log --format='%h %an <%ae> %s' -n 5


