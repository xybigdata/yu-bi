/*
 * YuBi
 * <p>
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package yubi.core.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageInfo implements Serializable {

    private Long pageSize;

    private Long pageNo;

    private Long total;

    private Boolean countTotal;

    public long getPageSize() {
        return pageSize == null ? 0 : pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize == null ? 0 : pageSize;
    }

    public long getPageNo() {
        return pageNo == null ? 0 : pageNo;
    }

    public void setPageNo(Long pageNo) {
        this.pageNo = pageNo == null ? 0 : pageNo;
    }

    public long getTotal() {
        return total == null ? 0 : total;
    }

    public void setTotal(Long total) {
        this.total = total == null ? 0 : total;
    }

    public boolean isCountTotal() {
        return countTotal != null && countTotal;
    }

    public void setCountTotal(Boolean countTotal) {
        this.countTotal = countTotal != null && countTotal;
    }

    @Override
    public String toString() {
        return "PageInfo{" +
                "pageSize=" + pageSize +
                ", pageNo=" + pageNo +
                ", total=" + total +
                '}';
    }
}
