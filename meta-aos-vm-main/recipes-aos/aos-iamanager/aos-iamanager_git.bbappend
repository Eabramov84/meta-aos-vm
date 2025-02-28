FILESEXTRAPATHS_prepend := "${THISDIR}/files:"

SRC_URI += " \
    file://aos-vis-service.conf \
"

AOS_IAM_IDENT_MODULES = " \
    identhandler/modules/visidentifier \
"

FILES_${PN} += " \
    ${sysconfdir}/systemd/system/aos-iamanager.service.d/ \
"

do_install_append() {
    install -d ${D}${sysconfdir}/systemd/system/aos-iamanager.service.d
    install -m 0644 ${WORKDIR}/aos-vis-service.conf ${D}${sysconfdir}/systemd/system/aos-iamanager.service.d/10-aos-vis-service.conf
}

python do_update_config_append() {
    # Add remote IAM's configuration

    data["RemoteIams"] = []

    for node in d.getVar("NODE_LIST").split():
        if node != d.getVar("NODE_ID"):
            data["RemoteIams"].append({"NodeID": node, "URL": node+":8089"})

    # Remove restarting nfs-server for single node

    if len(d.getVar("NODE_LIST").split()) <= 1:
        for index, cmd in enumerate(data["DiskEncryptionCmdArgs"]):
            data["DiskEncryptionCmdArgs"][index] = cmd.rstrip(" ; systemctl restart nfs-server.service")

    with open(file_name, "w") as f:
        json.dump(data, f, indent=4)
}
