package mate.academy.accommodationbookingservice.service.specification;

public interface SpecificationProviderManager<T> {
    SpecificationProvider<T> getSpecificationProvider(String key);
}
