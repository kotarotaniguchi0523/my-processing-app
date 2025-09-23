# PowerShell script to load API key from .env and run the application

# Check if .env file exists
if (-Not (Test-Path ".env")) {
    Write-Error ".env file not found. Please create a .env file with GOOGLE_API_KEY or GEMINI_API_KEY"
    exit 1
}

# Read .env file and set environment variables
Get-Content .env | ForEach-Object {
    if ($_ -match "^\s*([^#][^=]+)=(.*)$") {
        $key = $matches[1].Trim()
        $value = $matches[2].Trim()

        # Remove quotes if present
        if ($value -match '^"(.*)"$' -or $value -match "^'(.*)'$") {
            $value = $matches[1]
        }

        # Set environment variable
        [System.Environment]::SetEnvironmentVariable($key, $value, [System.EnvironmentVariableTarget]::Process)
        Write-Host "Set $key from .env file"
    }
}

# Get GEMINI_API_KEY (primary key for this project)
$geminiApiKey = [System.Environment]::GetEnvironmentVariable("GEMINI_API_KEY", [System.EnvironmentVariableTarget]::Process)

if (-Not $geminiApiKey) {
    Write-Error "No GEMINI_API_KEY found in .env file. Please add GEMINI_API_KEY to .env file"
    exit 1
}

# ADK requires GOOGLE_API_KEY, so we copy GEMINI_API_KEY to GOOGLE_API_KEY
[System.Environment]::SetEnvironmentVariable("GOOGLE_API_KEY", $geminiApiKey, [System.EnvironmentVariableTarget]::Process)
Write-Host "Mapped GEMINI_API_KEY to GOOGLE_API_KEY for ADK compatibility"

Write-Host "Starting application with ADK LLM Workflow..."
Write-Host "GOOGLE_API_KEY is set from .env file"

# Compile and run the application
mvn compile
if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed"
    exit 1
}

mvn exec:java