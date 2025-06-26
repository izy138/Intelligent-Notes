make sure maven is installed steps to open program:

MAC: if you dont have homebrew installed: enter this command to terminal:

/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
then follow the steps homebrew provides on the terminal it looks like:

==> Next steps: Run these commands in your terminal to add Homebrew to your PATH:

echo >> /Users/isabellacorrea/.zprofile

echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> /Users/isabellacorrea/.zprofile eval "$(/opt/homebrew/bin/brew shellenv)"

verify you did it correctly by: brew help if this command work ^ continue:

once homebrew is installed enter:

brew install maven
this will take a long time btw just let it install

check maven version:

mvn -version
WINDOWS: use chocolatey if you have that or homebrew for windows:

/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)" then: echo 'eval "$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)"' >> ~/.profile eval "$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)"
once homebrew is installed enter:

brew install maven
this will take a long time btw just let it install

check maven version:

mvn -version
Next: (you must do this command whenever a change is made to the codebase)

mvn clean compile
then to finally open the program:

mvn javafx:run
