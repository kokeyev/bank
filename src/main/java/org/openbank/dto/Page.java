package org.openbank.dto;

import java.util.List;

public class Page<T> {

  private final List<T> items;
  private final int currentPage;
  private final int totalPages;
  private final boolean hasPrevious;
  private final boolean hasNext;

  public Page(List<T> allItems, int page, int size) {
    int safeSize = Math.max(size, 1);
    int itemCount = allItems == null ? 0 : allItems.size();
    this.totalPages = Math.max(1, (int) Math.ceil((double) itemCount / safeSize));
    this.currentPage = Math.min(Math.max(page, 1), totalPages);
    int fromIndex = Math.min((currentPage - 1) * safeSize, itemCount);
    int toIndex = Math.min(fromIndex + safeSize, itemCount);
    this.items = allItems == null ? List.of() : allItems.subList(fromIndex, toIndex);
    this.hasPrevious = currentPage > 1;
    this.hasNext = currentPage < totalPages;
  }

  public Page(List<T> items, int page, int size, int totalItems) {
    int safeSize = Math.max(size, 1);
    this.totalPages = Math.max(1, (int) Math.ceil((double) Math.max(totalItems, 0) / safeSize));
    this.currentPage = Math.min(Math.max(page, 1), totalPages);
    this.items = items == null ? List.of() : items;
    this.hasPrevious = currentPage > 1;
    this.hasNext = currentPage < totalPages;
  }

  public List<T> getItems() {
    return items;
  }

  public int getCurrentPage() {
    return currentPage;
  }

  public int getTotalPages() {
    return totalPages;
  }

  public boolean isHasPrevious() {
    return hasPrevious;
  }

  public boolean getHasPrevious() {
    return hasPrevious;
  }

  public boolean isHasNext() {
    return hasNext;
  }

  public boolean getHasNext() {
    return hasNext;
  }
}
