import type { Product } from '../types.ts';

export const VERIFIED_PRODUCTS: Product[] = [
  {
    id: 'p-1',
    sku: 'RYZ-PRO-01',
    name: 'RYZORA Premium Pro Wireless Earbuds',
    banglaName: 'রাইজোরা প্রিমিয়াম প্রো ওয়্যারলেস এয়ারবাডস',
    description: 'Active Noise Cancellation, 36h Playtime, Deep Bass, IPX5 Waterproof.',
    category: 'Electronics',
    priceBdt: 2450,
    discountPriceBdt: 1990,
    availableSizes: ['Standard'],
    availableColors: ['Matte Black', 'Pearl White'],
    stockQuantity: 45,
    isActive: true,
  },
  {
    id: 'p-2',
    sku: 'RYZ-SW-02',
    name: 'RYZORA Apex AMOLED Smart Watch',
    banglaName: 'রাইজোরা অ্যাপেক্স অ্যামোলেড স্মার্ট ওয়াচ',
    description: '1.43 inch AMOLED Display, Bluetooth Calling, 100+ Sports Modes, Health Tracking.',
    category: 'Electronics',
    priceBdt: 3800,
    discountPriceBdt: 3250,
    availableSizes: ['Free Size'],
    availableColors: ['Midnight Black', 'Metallic Silver'],
    stockQuantity: 30,
    isActive: true,
  },
  {
    id: 'p-3',
    sku: 'RYZ-TEE-03',
    name: 'RYZORA Heavyweight Cotton Graphic Tee',
    banglaName: 'রাইজোরা হেভিওয়েট কটন টি-শার্ট',
    description: '220 GSM 100% Combed Compact Cotton, Pre-shrunk, Signature High-Density Print.',
    category: 'Fashion',
    priceBdt: 950,
    discountPriceBdt: 790,
    availableSizes: ['M', 'L', 'XL', 'XXL'],
    availableColors: ['Jet Black', 'Off White', 'Sage Green'],
    stockQuantity: 120,
    isActive: true,
  },
  {
    id: 'p-4',
    sku: 'RYZ-POLO-04',
    name: 'RYZORA Classic Pique Knit Polo',
    banglaName: 'রাইজোরা ক্লাসিক পোলো শার্ট',
    description: 'Premium Mercerized Cotton Pique, Structured Collar, Regular Fit.',
    category: 'Fashion',
    priceBdt: 1250,
    discountPriceBdt: 1050,
    availableSizes: ['M', 'L', 'XL'],
    availableColors: ['Navy Blue', 'Maroon', 'Olive Green'],
    stockQuantity: 85,
    isActive: true,
  },
  {
    id: 'p-5',
    sku: 'RYZ-BAG-05',
    name: 'RYZORA Urban Commuter Waterproof Backpack',
    banglaName: 'রাইজোরা আরবান ওয়াটারপ্রুফ ব্যাকপ্যাক',
    description: 'Fits up to 15.6 inch laptop, Hidden Anti-theft Pocket, Ergonomic Shoulder Straps.',
    category: 'Accessories',
    priceBdt: 2150,
    discountPriceBdt: 1750,
    availableSizes: ['20L'],
    availableColors: ['Obsidian Black', 'Steel Grey'],
    stockQuantity: 25,
    isActive: true,
  },
  {
    id: 'p-6',
    sku: 'RYZ-PB-06',
    name: 'RYZORA PowerCore 20000mAh 22.5W Fast Power Bank',
    banglaName: 'রাইজোরা পাওয়ারকোর ২০০০০ এমএএইচ পাওয়ার ব্যাংক',
    description: 'PD 3.0 & QC 4.0 Fast Charging, Digital Battery Indicator, Dual Type-C Output.',
    category: 'Electronics',
    priceBdt: 1950,
    discountPriceBdt: 1590,
    availableSizes: ['Standard'],
    availableColors: ['Space Grey'],
    stockQuantity: 60,
    isActive: true,
  },
];

export class CatalogService {
  private products: Product[] = [...VERIFIED_PRODUCTS];

  public getAllActiveProducts(): Product[] {
    return this.products.filter((p) => p.isActive);
  }

  public findProductByQuery(query: string): Product | undefined {
    const q = query.toLowerCase().trim();
    return this.products.find((p) => {
      const matchSku = p.sku.toLowerCase().includes(q);
      const matchName = p.name.toLowerCase().includes(q);
      const matchBangla = p.banglaName && p.banglaName.toLowerCase().includes(q);
      const matchCategory = p.category.toLowerCase().includes(q);
      const matchWords = q.split(/\s+/).some((w) => p.name.toLowerCase().includes(w) || (p.banglaName && p.banglaName.includes(w)));
      return matchSku || matchName || matchBangla || matchCategory || matchWords;
    });
  }

  public getProductById(id: string): Product | undefined {
    return this.products.find((p) => p.id === id);
  }

  public isStockAvailable(productId: string, quantity: number = 1): boolean {
    const product = this.getProductById(productId);
    if (!product || !product.isActive) return false;
    return product.stockQuantity >= quantity;
  }
}

export const catalogService = new CatalogService();
