#!/bin/bash

function send_notification() {
    echo "$1"
}

function assert_success() {
    "${@}"
    local status=${?}
    if [ ${status} -ne 0 ]; then
        send_notification "### Error ${status} at: ${BASH_LINENO[*]} ###"
        exit ${status}
    fi
}

app=wishlists
version=0.0.1
server=insanusmokrassar

assert_success ../gradlew clean
assert_success ../gradlew build
assert_success ../gradlew :wishlist.client:jsBrowserDistribution
assert_success tar -cf ./build/productionExecutable.tar -C ../client/build/dist/js/productionExecutable .


builder=${app}-multiarch
if ! sudo docker buildx inspect "$builder" > /dev/null 2>&1; then
    # Require docker-buildx
    assert_success sudo docker buildx create --name "$builder" --driver docker-container --bootstrap
fi
assert_success sudo docker buildx use "$builder"

assert_success sudo docker buildx build \
    --platform linux/amd64,linux/arm64 \
    -t $server/$app:"$version" \
    -t $server/$app:latest \
    -f Dockerfile \
    --push \
    .
