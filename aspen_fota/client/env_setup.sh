#!/bin/bash

#-- options
BUILDTYPE=Debug

#-- build type & SDK path
read -p "Select build type ( RELEASE / DEBUG ), default is DEBUG: " BUILDTYPE

if [ "$BUILDTYPE" == RELEASE ] || [ "$BUILDTYPE" == release ]; then
    BUILDTYPE=Release
elif [ "$BUILDTYPE" == "" ] || [ "$BUILDTYPE" == DEBUG ] || [ "$BUILDTYPE" == debug ]; then
    BUILDTYPE=Debug
else
    echo "Illegal argument! Set to default."
    BUILDTYPE=Debug
fi

echo ""; echo "Your Build Type is: $BUILDTYPE"

#-- create register file
androidsdk=""
crystaxndkpath=""
androidndkpath=""
opencvpath=""

test -e ./ds_env_var.sh && . ds_env_var.sh \
        && androidsdk=$ANDROID_HOME \
            && crystaxndkpath=$ANDROID_NDK \
                && androidndkpath=$ANDROID_NDK_HOME \
                     &&opencvpath=$OPENCV_MAKEFILE

echo "Your ANDROID_HOME (Android SDK path) is: $androidsdk"
echo "Your ANDROID_NDK (crystax-ndk path) is: $crystaxndkpath"
echo "Your ANDROID_NDK_ROOT (crystax-ndk path) is: $crystaxndkpath"
echo "Your ANDROID_NDK_HOME (Android NDK path) is: $androidndkpath"
echo "Your OPENCV_MAKEFILE (OpenCV makefile path) is: $opencvpath"

if [ "$androidsdk" == "" ] || 
    [ "$crystaxndkpath" == "" ] || 
    [ "$androidndkpath" == "" ] ||
    [ "$opencvpath" == "" ]; then

    if [ "$androidsdk" == "" ]; then
        read -p "Input path of Android SDK: " androidsdk
    fi
    if [ "$crystaxndkpath" == "" ]; then
        read -p "Input path of crystax-ndk: " crystaxndkpath
    fi
    if [ "$androidndkpath" == "" ]; then
        read -p "Input path of Android NDK: " androidndkpath
    fi
    if [ "$opencvpath" == "" ]; then
        read -p "Input path of OpenCV makefile: " opencvpath
    fi

    export ANDROID_HOME=$androidsdk
    export ANDROID_NDK=$crystaxndkpath
    export ANDROID_NDK_ROOT=$crystaxndkpath
    export ANDROID_NDK_HOME=$androidndkpath
    export OPENCV_MAKEFILE=$opencvpath

    echo "#!/bin/bash" > ./ds_env_var.sh
    echo "export ANDROID_HOME=$androidsdk" >> ./ds_env_var.sh
    echo "export ANDROID_NDK=$crystaxndkpath" >> ./ds_env_var.sh
    echo "export ANDROID_NDK_ROOT=$crystaxndkpath" >> ./ds_env_var.sh
    echo "export ANDROID_NDK_HOME=$androidndkpath" >> ./ds_env_var.sh
    echo "export OPENCV_MAKEFILE=$opencvpath" >> ./ds_env_var.sh
fi

MAKECMD=(
    clean
    all)

MODULECMD=(
    create
    delete)

htcvr_ds_module() {
    if [ $# -ge 1 ]; then
        case $1 in
            ${MODULECMD[0]})
                echo "create new module $2"
                if [ -d $2 ]; then
                    echo "$2 is exist, abort!!"
                else
                    mkdir $2
                    cp -rf templete/* $2
                    mkdir $2/src/main/java/htc/com/vr/$2
                    sed -i "s/xxxx/$2/g" $2/build.gradle
                    # tree $2
                    echo "Auto-gen build.gradle..."
                    # cat $2/build.gradle
                fi
                ;;

            ${MODULECMD[2]})
                echo "delete $2"
                rm -rf $2
                ;;
        esac
    fi
}

clean() {
    find ./ -name 'build' | xargs rm -rf $1
    find ./ -name '.gradle' | xargs rm -rf $1
    find ./ -name 'libs' | xargs rm -rf $1
    find ./ -name 'obj' | xargs rm -rf $1
}

# --autoComplete for tab
Complete_Module() {
    local cur
    COMPREPLY=()
    cur=${COMP_WORDS[COMP_CWORD]}

    case "$cur" in
        *)
            COMPREPLY=( $( compgen -W '${MODULECMD[0]} ${MODULECMD[1]} ' -- $cur ) )
            ;;
    esac
}
Complete_Make() {
    local cur
    COMPREPLY=()
    cur=${COMP_WORDS[COMP_CWORD]}

    case "$cur" in
        *)
            COMPREPLY=( $( compgen -W '${MAKECMD[0]} ${MAKECMD[1]}' -- $cur ) )
            ;;
    esac
}

complete -F Complete_Module -o filenames htcvr_ds_module
complete -F Complete_Make -o filenames htcvr_ds_make
