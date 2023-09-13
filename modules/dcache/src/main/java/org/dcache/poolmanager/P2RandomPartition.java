/* dCache - http://www.dcache.org/
 *
 * Copyright (C) 2023 Deutsches Elektronen-Synchrotron
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.dcache.poolmanager;

import static java.util.function.Predicate.not;
import diskCacheV111.poolManager.CostModule;
import diskCacheV111.util.CacheException;
import org.dcache.pool.assumption.AvailableSpaceAssumption;
import org.dcache.vehicles.FileAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Partition that uses 'power of two random' selection on write and random pool on read.
 * The 'power of two random' works as: select two random pools and pick the one with lowest
 * space cost. The space cost calculates as `(removable+free)/total`.
 *
 * <p></p>Based on 'ON THE POWER OF TWO CHOICES: BALLS AND BINS IN CONTINUOUS TIME'</p>
 *
 * @see https://arxiv.org/pdf/math/0508451.pdf
 */
public class P2RandomPartition extends Partition {

    public final static String TYPE = "p2random";
    private static final long serialVersionUID = 5005233401277944842L;

    public P2RandomPartition(Map<String, String> inherited) {
        super(NO_PROPERTIES, inherited, NO_PROPERTIES);
    }

    @Override
    protected Partition create(Map<String, String> inherited,
                               Map<String, String> properties) {
        return new P2RandomPartition(inherited);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public P2pPair selectPool2Pool(CostModule cm, List<PoolInfo> src, List<PoolInfo> dst, FileAttributes attributes, boolean force) throws CacheException {

        Collections.shuffle(src);

        /*
         * for randomly selected source pool and try to find correct destination.
         */
        for (PoolInfo srcPoolInfo : src) {

            // get the pools that are on a different host
            List<PoolInfo> tryList = dst.stream()
                    .filter(not(d -> d.getHostName().equals(srcPoolInfo.getHostName())))
                    .collect(Collectors.toList());

            if (!tryList.isEmpty()) {
                SelectedPool destPoolInfo = selectWritePool(cm, tryList, attributes, attributes.getSize());
                return new P2pPair(new SelectedPool(srcPoolInfo), destPoolInfo);
            }
        }

        // if all sources and destinations on the same hosts, then just pic a random one.
        SelectedPool destPoolInfo = selectWritePool(cm, dst, attributes, attributes.getSize());
        return new P2pPair(new SelectedPool(src.get(0)), destPoolInfo);
    }

    @Override
    public SelectedPool selectStagePool(CostModule cm, List<PoolInfo> pools, Optional<PoolInfo> previous, FileAttributes attributes) throws CacheException {
        return selectWritePool(cm, pools, attributes, attributes.getSize());
    }

    @Override
    public SelectedPool selectWritePool(CostModule cm, List<PoolInfo> pools, FileAttributes attributes, long preallocated) throws CacheException {

        // select two random pools and select the one with the lowest space cost
        int index1 = ThreadLocalRandom.current().nextInt(pools.size());
        int index2 = ThreadLocalRandom.current().nextInt(pools.size());

        PoolInfo selection = pools.get(index1);
        PoolInfo alternative = pools.get(index2);

        // lower cost is better
        if (Double.compare(toSpaceCost(selection), toSpaceCost(alternative)) > 0) {
            selection = alternative;
        }

        return new SelectedPool(selection, new AvailableSpaceAssumption(preallocated));
    }

    @Override
    public SelectedPool selectReadPool(CostModule cm, List<PoolInfo> pools, FileAttributes attributes) throws CacheException {
        return new SelectedPool(pools.get(ThreadLocalRandom.current().nextInt(pools.size())));
    }


    private static double toSpaceCost(PoolInfo poolInfo) {

        long spaceToUse = poolInfo.getCostInfo().getSpaceInfo().getFreeSpace()
                + poolInfo.getCostInfo().getSpaceInfo().getRemovableSpace();

        return (double) spaceToUse / poolInfo.getCostInfo().getSpaceInfo().getTotalSpace();
    }
}
