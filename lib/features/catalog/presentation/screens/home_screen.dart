import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:rang_shop/core/state/app_cubits.dart';
import 'package:rang_shop/core/utils/price_formatter.dart';
import 'package:rang_shop/features/catalog/data/repositories/mock_product_repository.dart';
import 'package:rang_shop/features/catalog/domain/entities/product_entity.dart';
import 'package:rang_shop/features/catalog/presentation/cubit/home_cubit.dart';


const Color _discountColor = Color(0xFFE5173F);
const Color _starColor = Color(0xFFFFB800);


String _tr(BuildContext context, String ru, String tg) =>
    Localizations.localeOf(context).languageCode == 'tg' ? tg : ru;


void _showSnack(BuildContext context, String message) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(
      SnackBar(
        content: Text(message),
        behavior: SnackBarBehavior.floating,
        duration: const Duration(seconds: 2),
      ),
    );
}


void _showSoon(BuildContext context) =>
    _showSnack(context, _tr(context, 'В разработке', 'Дар ҳоли таҳия'));


class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});


  @override
  Widget build(BuildContext context) {
    return BlocProvider<HomeCubit>(
      create: (_) => HomeCubit(MockProductRepository())..loadProducts(),
      child: const _HomeView(),
    );
  }
}


class _HomeView extends StatelessWidget {
  const _HomeView();


  @override
  Widget build(BuildContext context) {
    return SafeArea(
      bottom: false,
      child: CustomScrollView(
        slivers: [
          const SliverToBoxAdapter(child: _HomeHeader()),
          const SliverToBoxAdapter(child: _SearchField()),
          const SliverToBoxAdapter(child: _PromoBanner()),
          const SliverToBoxAdapter(child: _CategoriesRow()),
          const SliverToBoxAdapter(child: _SectionHeader()),
          BlocBuilder<HomeCubit, HomeState>(
            builder: (context, state) {
              if (state.status == HomeStatus.loading) {
                return const SliverToBoxAdapter(
                  child: Padding(
                    padding: EdgeInsets.all(48),
                    child: Center(child: CircularProgressIndicator()),
                  ),
                );
              }
              if (state.status == HomeStatus.failure) {
                return SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.all(32),
                    child: Center(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(
                            state.errorMessage ?? '',
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 8),
                          IconButton(
                            icon: const Icon(Icons.refresh),
                            onPressed: () =>
                                context.read<HomeCubit>().loadProducts(),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              }
              return SliverPadding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                sliver: SliverGrid(
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 2,
                    mainAxisSpacing: 12,
                    crossAxisSpacing: 12,
                    mainAxisExtent: 310,
                  ),
                  delegate: SliverChildBuilderDelegate(
                    (context, index) =>
                        _ProductCard(product: state.products[index]),
                    childCount: state.products.length,
                  ),
                ),
              );
            },
          ),
          const SliverToBoxAdapter(child: SizedBox(height: 100)),
        ],
      ),
    );
  }
}


class _HomeHeader extends StatelessWidget {
  const _HomeHeader();


  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 8, 0),
      child: Row(
        children: [
          Text(
            _tr(context, 'Душанбе', 'Душанбе'),
            style: theme.textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.bold,
            ),
          ),
          const Icon(Icons.keyboard_arrow_down),
          const Spacer(),
          IconButton(
            icon: const Icon(Icons.notifications_none),
            onPressed: () => _showSoon(context),
          ),
        ],
      ),
    );
  }
}


class _SearchField extends StatelessWidget {
  const _SearchField();


  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;


    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
      child: Material(
        color: scheme.surfaceContainerHighest,
        shape: StadiumBorder(side: BorderSide(color: scheme.outline)),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: () => _showSoon(context),
          child: SizedBox(
            height: 48,
            child: Row(
              children: [
                const SizedBox(width: 16),
                Icon(Icons.search, color: scheme.onSurfaceVariant),
                const SizedBox(width: 10),
                Text(
                  _tr(context, 'Поиск товаров...', 'Ҷустуҷӯи маҳсулот...'),
                  style: TextStyle(color: scheme.onSurfaceVariant),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}


class _PromoBanner extends StatelessWidget {
  const _PromoBanner();


  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);


    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
      child: Container(
        height: 152,
        clipBehavior: Clip.antiAlias,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(24),
          gradient: LinearGradient(
            colors: [theme.colorScheme.primary, const Color(0xFF4B1A8F)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
        ),
        child: Stack(
          children: [
            const Positioned(
              right: -8,
              bottom: -8,
              child: Icon(
                Icons.checkroom,
                size: 120,
                color: Color(0x33FFFFFF),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  SizedBox(
                    width: 190,
                    child: Text(
                      _tr(
                        context,
                        'Новая коллекция 2025',
                        'Коллексияи нав 2025',
                      ),
                      maxLines: 3,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.titleLarge?.copyWith(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        height: 1.15,
                      ),
                    ),
                  ),
                  FilledButton(
                    onPressed: () => _showSoon(context),
                    style: FilledButton.styleFrom(
                      backgroundColor: const Color(0x40FFFFFF),
                      foregroundColor: Colors.white,
                      minimumSize: const Size(0, 36),
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      shape: const StadiumBorder(),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(_tr(context, 'Перейти', 'Гузаштан')),
                        const SizedBox(width: 6),
                        const Icon(Icons.arrow_forward, size: 16),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}


class _CategoriesRow extends StatelessWidget {
  const _CategoriesRow();


  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
      child: Row(
        children: [
          Expanded(
            child: _CategoryItem(
              icon: Icons.checkroom,
              label: _tr(context, 'Одежда', 'Либос'),
            ),
          ),
          Expanded(
            child: _CategoryItem(
              icon: Icons.directions_run,
              label: _tr(context, 'Обувь', 'Пойафзол'),
            ),
          ),
          Expanded(
            child: _CategoryItem(
              icon: Icons.work_outline,
              label: _tr(context, 'Аксессуары', 'Аксессуарҳо'),
            ),
          ),
          Expanded(
            child: _CategoryItem(
              icon: Icons.face_retouching_natural,
              label: _tr(context, 'Красота', 'Зебоӣ'),
            ),
          ),
        ],
      ),
    );
  }
}


class _CategoryItem extends StatelessWidget {
  const _CategoryItem({required this.icon, required this.label});


  final IconData icon;
  final String label;


  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;


    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: () => _showSoon(context),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 60,
            height: 60,
            decoration: BoxDecoration(
              color: scheme.surfaceContainerHighest,
              borderRadius: BorderRadius.circular(18),
              border: Border.all(color: scheme.outline),
            ),
            child: Icon(icon, color: scheme.secondary),
          ),
          const SizedBox(height: 6),
          Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: theme.textTheme.bodySmall,
          ),
        ],
      ),
    );
  }
}


class _SectionHeader extends StatelessWidget {
  const _SectionHeader();


  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);


    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 20, 8, 8),
      child: Row(
        children: [
          Expanded(
            child: Text(
              _tr(context, 'Популярные товары', 'Маҳсулоти маъмул'),
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          TextButton(
            onPressed: () => _showSoon(context),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(_tr(context, 'Все', 'Ҳама')),
                const SizedBox(width: 4),
                const Icon(Icons.arrow_forward, size: 16),
              ],
            ),
          ),
        ],
      ),
    );
  }
}


class _ProductCard extends StatelessWidget {
  const _ProductCard({required this.product});


  final ProductEntity product;


  void _onAddToCart(BuildContext context) {
    final isAuthorized = context.read<AuthSessionCubit>().state;
    if (!isAuthorized) {
      context.push('/auth');
      return;
    }
    context.read<CartCubit>().add(product);
    _showSnack(
      context,
      _tr(context, 'Добавлено в корзину', 'Ба сабад илова шуд'),
    );
  }


  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;


    return Material(
      color: scheme.surface,
      borderRadius: BorderRadius.circular(20),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => _showSoon(context),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Stack(
                fit: StackFit.expand,
                children: [
                  _ProductImage(product: product),
                  if (product.hasDiscount)
                    Positioned(
                      left: 8,
                      top: 8,
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 4,
                        ),
                        decoration: BoxDecoration(
                          color: _discountColor,
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Text(
                          '-${product.discountPercent}%',
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 11,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ),
                  Positioned(
                    right: 8,
                    top: 8,
                    child: BlocSelector<FavoritesCubit, List<ProductEntity>,
                        bool>(
                      selector: (items) =>
                          items.any((item) => item.id == product.id),
                      builder: (context, isFavorite) {
                        return GestureDetector(
                          behavior: HitTestBehavior.opaque,
                          onTap: () =>
                              context.read<FavoritesCubit>().toggle(product),
                          child: Container(
                            width: 32,
                            height: 32,
                            decoration: const BoxDecoration(
                              color: Color(0x66000000),
                              shape: BoxShape.circle,
                            ),
                            child: Icon(
                              isFavorite
                                  ? Icons.favorite
                                  : Icons.favorite_border,
                              size: 18,
                              color:
                                  isFavorite ? scheme.secondary : Colors.white,
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    product.name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodyMedium?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          PriceFormatter.formatFromDirams(product.priceDirams),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: theme.textTheme.bodyMedium?.copyWith(
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      const Icon(Icons.star, size: 14, color: _starColor),
                      const SizedBox(width: 2),
                      Text(
                        product.rating.toStringAsFixed(1),
                        style: theme.textTheme.bodySmall,
                      ),
                    ],
                  ),
                  SizedBox(
                    height: 18,
                    child: product.hasDiscount
                        ? Text(
                            PriceFormatter.formatFromDirams(
                              product.oldPriceDirams!,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: scheme.onSurfaceVariant,
                              decoration: TextDecoration.lineThrough,
                            ),
                          )
                        : null,
                  ),
                  const SizedBox(height: 8),
                  SizedBox(
                    width: double.infinity,
                    height: 36,
                    child: FilledButton(
                      onPressed: () => _onAddToCart(context),
                      style: FilledButton.styleFrom(
                        minimumSize: const Size(0, 36),
                        padding: EdgeInsets.zero,
                        shape: const StadiumBorder(),
                        textStyle: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      child: Text(_tr(context, 'В корзину', 'Ба сабад')),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}


class _ProductImage extends StatelessWidget {
  const _ProductImage({required this.product});


  final ProductEntity product;


  static IconData _iconFor(String category) {
    switch (category) {
      case 'shoes':
        return Icons.directions_run;
      case 'accessories':
        return Icons.work_outline;
      case 'beauty':
        return Icons.face_retouching_natural;
      case 'clothes':
      default:
        return Icons.checkroom;
    }
  }


  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;


    final placeholder = DecoratedBox(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [scheme.primaryContainer, scheme.surfaceContainerHighest],
        ),
      ),
      child: Center(
        child: Container(
          width: 84,
          height: 84,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: scheme.surface.withValues(alpha: 0.35),
            border: Border.all(color: scheme.secondary.withValues(alpha: 0.5)),
          ),
          child: Icon(
            _iconFor(product.category),
            size: 44,
            color: scheme.secondary,
          ),
        ),
      ),
    );


    final url = product.imageUrl;
    if (url == null || url.isEmpty) return placeholder;


    return Image.network(
      url,
      fit: BoxFit.cover,
      errorBuilder: (context, error, stackTrace) => placeholder,
    );
  }
}
