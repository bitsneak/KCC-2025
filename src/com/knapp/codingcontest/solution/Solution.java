/* -*- java -*-
# =========================================================================== #
#                                                                             #
#                         Copyright (C) KNAPP AG                              #
#                                                                             #
#       The copyright to the computer program(s) herein is the property       #
#       of Knapp.  The program(s) may be used   and/or copied only with       #
#       the  written permission of  Knapp  or in  accordance  with  the       #
#       terms and conditions stipulated in the agreement/contract under       #
#       which the program(s) have been supplied.                              #
#                                                                             #
# =========================================================================== #
*/

package com.knapp.codingcontest.solution;

import java.util.*;
import java.util.stream.Collectors;

import com.knapp.codingcontest.core.InputDataInternal;
import com.knapp.codingcontest.core.WarehouseInternal;
import com.knapp.codingcontest.data.Bin;
import com.knapp.codingcontest.data.InputData;
import com.knapp.codingcontest.data.Institute;
import com.knapp.codingcontest.data.Order;
import com.knapp.codingcontest.data.Position;
import com.knapp.codingcontest.data.Shelf;
import com.knapp.codingcontest.operations.CostFactors;
import com.knapp.codingcontest.operations.InfoSnapshot;
import com.knapp.codingcontest.operations.InfoSnapshot.OperationType;
import com.knapp.codingcontest.operations.Warehouse;
import com.knapp.codingcontest.operations.ex.InvalidBinException;
import com.knapp.codingcontest.operations.ex.NoOrderAssignedToMpsBinException;
import com.knapp.codingcontest.operations.ex.NoSuchProductInOrderException;
import com.knapp.codingcontest.operations.ex.NoSuchProductInOrderLeftException;

/**
 * This is the code YOU have to provide
 */
public class Solution {
  public String getParticipantName() {
    return ""; // TODO: return your name
  }

  public Institute getParticipantInstitution() {
    return Institute.Sonstige; // TODO: return the Id of your institute - please refer to the hand-out
  }

  // ----------------------------------------------------------------------------

  protected final Warehouse warehouse;
  protected final InputData input;

  // ----------------------------------------------------------------------------

  public Solution(final WarehouseInternal iwarehouse, final InputDataInternal iinput) {
    // TODO: prepare data structures (but may also be done in run() method below)
    warehouse = iwarehouse;
    input = iinput;
    if (getParticipantName() == null) {
      throw new IllegalArgumentException("let getParticipantName() return your name");
    }
    if (getParticipantInstitution() == null) {
      throw new IllegalArgumentException("let getParticipantInstitution() return yout institution");
    }
  }

  // ----------------------------------------------------------------------------

  /**
   * The main entry-point.
   *
   */
  public void run() throws Exception {
    List<Order> allOrders = input.getAllOrders().stream().toList();

    for (Order order : allOrders) {
      // group open products by type and count them
      Map<String, Integer> productCounts = new HashMap<>();
      for (String product : order.getOpenProducts()) {
        productCounts.put(product, productCounts.getOrDefault(product, 0) + 1);
      }

      Bin currentAssignedBin = null;

      // process product groups until all are done
      while (!productCounts.isEmpty()) {
        // get current position
        Position currentPosition = warehouse.getCurrentPosition();

        String bestProduct = null;
        int bestCount = 0;
        Shelf bestShelf = null;
        Bin bestCandidateBin = null;
        double minTotalCost = Double.MAX_VALUE;

        // evaluate each product group
        for (Map.Entry<String, Integer> entry : productCounts.entrySet()) {
          String product = entry.getKey();
          int count = entry.getValue();
          List<Shelf> shelves = warehouse.getProductShelves().getOrDefault(product, Collections.emptyList());

          // for each shelf that holds the product
          for (Shelf shelf : shelves) {
            double costToShelf = warehouse.calcCost(currentPosition, shelf.getPosition());

            // prepare candidate bins - always include currently assigned bin and any available bins
            List<Bin> candidateBins = new ArrayList<>();
            if (currentAssignedBin != null) {
              candidateBins.add(currentAssignedBin);
            }
            candidateBins.addAll(
                    warehouse.getAllBins().stream()
                            .filter(bin -> warehouse.getOrderAssignedToBin(bin) == null)
                            .collect(Collectors.toList())
            );

            // evaluate cost of going from currentPosition to shelf to candidate bin
            for (Bin candidateBin : candidateBins) {
              double costShelfToBin = warehouse.calcCost(shelf.getPosition(), candidateBin.getPosition());
              double totalCost = costToShelf + costShelfToBin;
              if (totalCost < minTotalCost) {
                minTotalCost = totalCost;
                bestProduct = product;
                bestCount = count;
                bestShelf = shelf;
                bestCandidateBin = candidateBin;
              }
            }
          }
        }

        // move to shelf and pick the product
        warehouse.pickProduct(bestShelf, bestProduct);

        // reassign order to new bin if needed
        if (currentAssignedBin == null || !currentAssignedBin.equals(bestCandidateBin)) {
          warehouse.assignOrder(order, bestCandidateBin);
          currentAssignedBin = bestCandidateBin;
        }

        // return to bin by putting all required product units
        for (int i = 0; i < bestCount; i++) {
          warehouse.putProduct(currentAssignedBin);
        }

        // remove the processed product group
        productCounts.remove(bestProduct);
      }
      // finish order
      warehouse.finishOrder(order);
    }
  }
  
  // ----------------------------------------------------------------------------
  // ----------------------------------------------------------------------------

  /**
   * Just for documentation purposes.
   *
   * Method may be removed without any side-effects
   *   divided into these sections
   *
   *     <li><em>input methods</em>
   *
   *     <li><em>main interaction methods</em>
   *         - these methods are the ones that make (explicit) changes to the warehouse
   *
   *     <li><em>information</em>
   *         - information you might need for your solution
   *
   *     <li><em>additional information</em>
   *         - various other infos: statistics, information about (current) costs, ...
   *
   */
  @SuppressWarnings("unused")
  private void apis() throws Exception {
    // ----- input -----

    final Collection<Order> orders = input.getAllOrders();
    final List<Bin> bins = warehouse.getAllBins();
    final List<Shelf> shelves = warehouse.getAllShelves();

    //
    final Order order = orders.iterator().next();
    final String product = order.getOpenProducts().get(0);
    final Bin bin = bins.get(0);

    final Map<String, List<Shelf>> productShelves = warehouse.getProductShelves();
    final Shelf shelf = productShelves.get(product).iterator().next();

    // ----- main interaction methods -----

    warehouse.assignOrder(order, bin);
    warehouse.pickProduct(shelf, product);
    warehouse.putProduct(bin);
    warehouse.finishOrder(order);

    // ----- information -----

    final List<String> aps = order.getAllProducts();
    final List<String> ops = order.getOpenProducts();

    final boolean ofin = warehouse.isOrderFinished(order);

    final Position currentPos = warehouse.getCurrentPosition();
    final double costa = warehouse.calcCost(currentPos, shelf.getPosition());
    final double costb = warehouse.calcCost(shelf.getPosition(), bin.getPosition());

    final Order bo = warehouse.getOrderAssignedToBin(bin);
    final List<Bin> obins = warehouse.getBinsForOrder(bo);

    // ----- additional information -----

    final CostFactors costFactors = input.getCostFactors();

    final double cf_up = costFactors.getUnfinishedProductPenalty();
    final double cf_d = costFactors.getDistanceCost();
    final double cf_sc = costFactors.getSideChangeCost();

    final InfoSnapshot info = warehouse.getInfoSnapshot();

    final int up = info.getUnfinishedProductCount();
    final int oao = info.getOperationCount(OperationType.AssignOrder);
    final int opip = info.getOperationCount(OperationType.PickProduct);
    final int opup = info.getOperationCount(OperationType.PutProduct);
    final int oo = info.getOperationCount(OperationType.FinishOrder);

    final double d = info.getDistance();
    final double sc = info.getCountSideChange();

    final double c_uo = info.getUnfinishedOrdersCost();
    final double c_d = info.getDistanceCost();
    final double c_sc = info.getSideChangeCost();
    final double c_t = info.getTotalCost();
  }

  // ----------------------------------------------------------------------------
}
