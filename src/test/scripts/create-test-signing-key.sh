#!/bin/bash
#
# This script generates a dummy signing key used for testing
#

export GNUPGHOME=$(realpath $(mktemp -d ./_action_gpg.XXX))
echo GNUPGHOME=$GNUPGHOME

dummyPassword=unitTesting

cat > $GNUPGHOME/key_gen_args.txt << EOF
Key-Type: RSA
Key-Length: 1024
Key-Usage: sign,cert
Subkey-Type: RSA 
Subkey-Usage: sign
Passphrase: $dummyPassword
Name-Real: Action Maven Publish
Name-Comment: Signing key for unit testing
Name-Email: jskov@mada.dk
Expire-Date: 1y
EOF

cat $GNUPGHOME/key_gen_args.txt | gpg -v --batch --gen-key 

# gpg -K --with-keygrip

# Delete the primary private key
keyGrp=$(gpg -K --with-keygrip --with-colons | grep grp | head -n 1 | cut -d: -f 10)
rm $GNUPGHOME/private-keys-v1.d/${keyGrp}.key

echo $dummyPassword | gpg --batch --pinentry-mode loopback --passphrase-fd 0 --export-secret-keys -a  > src/test/resources/gpg-testkey.txt
echo $dummyPassword > src/test/resources/gpg-testkey-password.txt
