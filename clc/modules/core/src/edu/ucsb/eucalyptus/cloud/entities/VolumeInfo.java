/*
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.*;

/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 *
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms, with
 *   or without modification, are permitted provided that the following
 *   conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *   IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *   OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *   EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *   PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *   PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *   LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *   NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *   THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *   LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *   SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *   BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *   THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *   OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *   WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *   ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/

@Entity
@Table( name = "Volumes" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class VolumeInfo {
    @Id
    @GeneratedValue
    @Column(name = "volume_id")
    private Long id = -1l;
    @Column(name = "volume_user_name")
    private String userName;
    @Column(name = "volume_name")
    private String volumeId;
    @Column(name = "size")
    private Integer size; //in GB
    @Column(name = "status")
    private String status;
    @Column(name = "create_time")
    private Date createTime;
    @Column(name = "zone")
    private String zone;
    @Column(name = "volume_bucket")
    private String volumeBucket;
    @Column(name = "volume_key")
    private String volumeKey;
    @Column(name = "snapshot_id")
    private String snapshotId;
    @Column(name = "transferred")
    private Boolean transferred;
    @Column(name = "duped_volume_name")
    private String dupedVolumeId;

    @OneToMany( cascade = CascadeType.ALL )
    @JoinTable(
            name = "volume_has_attachments",
            joinColumns = { @JoinColumn( name = "volume_id" ) },
            inverseJoinColumns = @JoinColumn( name = "attached_volume_id" )
    )
    @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
    private List<AttachedVolumeInfo> attachmentSet = new ArrayList<AttachedVolumeInfo>();

    public VolumeInfo() {}

    public VolumeInfo(String volumeId) {
        this.volumeId = volumeId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public List<AttachedVolumeInfo> getAttachmentSet() {
        return attachmentSet;
    }

    public void setAttachmentSet(ArrayList<AttachedVolumeInfo> attachmentSet) {
        this.attachmentSet = attachmentSet;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getVolumeBucket() {
        return volumeBucket;
    }

    public void setVolumeBucket(String volumeBucket) {
        this.volumeBucket = volumeBucket;
    }

    public String getVolumeKey() {
        return volumeKey;
    }

    public void setVolumeKey(String volumeKey) {
        this.volumeKey = volumeKey;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public Boolean getTransferred() {
        return transferred;
    }

    public void setTransferred(Boolean transferred) {
        this.transferred = transferred;
    }

    public String getDupedVolumeId() {
        return dupedVolumeId;
    }

    public void setDupedVolumeId(String dupedVolumeId) {
        this.dupedVolumeId = dupedVolumeId;
    }

    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        VolumeInfo that = ( VolumeInfo ) o;

        if ( !volumeId.equals( that.volumeId ) ) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return volumeId.hashCode();
    }

}
