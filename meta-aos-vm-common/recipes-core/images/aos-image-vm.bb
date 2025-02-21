SUMMARY = "An image which contains AOS components"
LICENSE = "Apache-2.0"

IMAGE_LINGUAS = " "

inherit core-image extrausers ${@bb.utils.contains('DISTRO_FEATURES', 'selinux', 'selinux-image', '', d)}

IMAGE_INSTALL = "packagegroup-core-boot kernel-modules ${CORE_IMAGE_EXTRA_INSTALL}"
IMAGE_FSTYPES = "tar.bz2 wic.vmdk"

IMAGE_FEATURES_append = " read-only-rootfs"

# Set password to the root user. This is the requirement of the provisioning script.
EXTRA_USERS_PARAMS = "usermod -P Password1 -s /bin/bash root;"

# System packages
IMAGE_INSTALL_append = " \
    bash \
    iperf3 \
    mc \
    netconfig \
    openssh \
    tzdata \
    ${@bb.utils.contains('DISTRO_FEATURES', 'selinux', ' \
        packagegroup-selinux-minimal \
        packagegroup-selinux-policycoreutils \
        aos-selinux-autorelabel \
        ', '', d)} \
"

# Variables
INITRAMFS_BOOT_PARAMS = " \
    vardir.disk=/dev/hda5 opendisk.target=/dev/hda6 opendisk.pkcs11=softhsm \
    opendisk.pkcs11.pinfile=/var/aos/iam/.usrpin aosupdate.disk=/dev/aosvg/workdirs aosupdate.path=um/update_rootfs \
    ${@bb.utils.contains('DISTRO_FEATURES', 'selinux', 'selinux.module=/usr/share/selinux/aos/base.pp', '', d)} \
"
BUILD_WIC_DIR = "${WORKDIR}/build-wic"
# In case of usage exclude directive in wks.in file, bitbake
# tries to copy files without SELinux labeling savings, so avoid using
# it with enabled SELinux.
ROOTFS_EXCLUDE_PATHS = "${@bb.utils.contains('DISTRO_FEATURES', 'selinux', '', '--exclude-path ./var/ ./home/', d)}"
SHARED_RESOURCE_DIR = "${TMPDIR}/work-shared/${IMAGE_BASENAME}-${MACHINE}"

ROOTFS_POSTPROCESS_COMMAND_append += "set_rootfs_version;"

# Dependencies

do_create_shared_links[cleandirs] = "${SHARED_RESOURCE_DIR}"

# Tasks

set_rootfs_version() {
    install -d ${IMAGE_ROOTFS}/etc/aos

    echo "VERSION=\"${ROOTFS_IMAGE_VERSION}\"" > ${IMAGE_ROOTFS}/etc/aos/version
}

# We need to have shared resources in work-shared dir for the layer and update functionality
# Creating symlink IMAGE_ROOTFS and BUILD_WIC_DIR to work-shared to get an access to them by
# layers and update
do_create_shared_links() {
    if [ -d ${IMAGE_ROOTFS} ]; then
        lnr ${IMAGE_ROOTFS} ${SHARED_RESOURCE_DIR}/rootfs
    fi

    if [ -d ${BUILD_WIC_DIR} ]; then
        lnr ${BUILD_WIC_DIR} ${SHARED_RESOURCE_DIR}/build-wic
    fi 
}

addtask create_shared_links after do_image_wic before do_image_complete
