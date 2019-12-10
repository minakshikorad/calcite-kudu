package com.twilio.raas.sql.rel;

import com.twilio.raas.sql.KuduRel;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.core.Sort;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.util.trace.CalciteTrace;
import org.slf4j.Logger;

/**
 * This relation sets {@link Implementor#sorted} to true and conditionally sets
 * {@link Implementor#limit} and {@link Implementor#offset}.
 */
public class KuduSortRel extends Sort implements KuduRel {

    public static final Logger LOGGER = CalciteTrace.getPlannerTracer();
    public final boolean groupByLimited;

    public KuduSortRel(RelOptCluster cluster, RelTraitSet traitSet,
                         RelNode child, RelCollation collation, RexNode offset, RexNode fetch) {
      this(cluster, traitSet, child, collation, offset, fetch, false);
    }

  public KuduSortRel(RelOptCluster cluster, RelTraitSet traitSet,
      RelNode child, RelCollation collation, RexNode offset, RexNode fetch, boolean groupByLimited) {
      super(cluster, traitSet, child, collation, offset, fetch);
      assert getConvention() == KuduRel.CONVENTION;
      assert getConvention() == child.getConvention();
      this.groupByLimited = groupByLimited;
    }

    @Override
    public Sort copy(RelTraitSet traitSet, RelNode input,
                               RelCollation newCollation, RexNode offset, RexNode fetch) {
      return new KuduSortRel(getCluster(), traitSet, input, collation, offset, fetch, groupByLimited);
    }

    @Override
    public RelWriter explainTerms(RelWriter pw) {
      super.explainTerms(pw);
      pw.item("groupByLimited", groupByLimited);
      return pw;
    }

    @Override
    public RelOptCost computeSelfCost(RelOptPlanner planner,
                                      RelMetadataQuery mq) {
      return planner.getCostFactory().makeZeroCost();
    }

    @Override
    public void implement(Implementor implementor) {
        implementor.visitChild(0, getInput());
        // create a sorted enumerator
        implementor.sorted = true;
        // set the offset
        if (offset != null ) {
            final RexLiteral parsedOffset = (RexLiteral) offset;
            final Long properOffset = (Long)parsedOffset.getValue2();
            implementor.offset = properOffset;
        }
        // set the limit
        if (fetch != null) {
            final RexLiteral parsedFetch = (RexLiteral) fetch;
            final Long properFetch = (Long)parsedFetch.getValue2();
            implementor.limit = properFetch;
        }

        implementor.groupByLimited = groupByLimited;
    }
}
