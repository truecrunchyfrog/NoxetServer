get-content .env | foreach {
  $name, $value = $_.split('=');
  set-content env:\$name $value;
};

$client = New-Object System.Net.WebClient;
$client.Credentials = New-Object System.Net.NetworkCredential($Env:USER, $Env:PASS);
$client.UploadFile($Env:DESTINATION, $Env:SOURCE);